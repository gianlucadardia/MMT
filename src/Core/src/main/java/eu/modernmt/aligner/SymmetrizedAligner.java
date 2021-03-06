package eu.modernmt.aligner;

import eu.modernmt.aligner.symal.GrowDiagonalFinalAndStrategy;
import eu.modernmt.aligner.symal.SymmetrizationStrategy;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.AlignmentsInterpolator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Created by davide on 09/05/16.
 */
public class SymmetrizedAligner implements Aligner {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Aligner forwardModel;
    private final Aligner backwardModel;
    private SymmetrizationStrategy strategy;

    public SymmetrizedAligner(Aligner forwardModel, Aligner backwardModel) {
        this.forwardModel = forwardModel;
        this.backwardModel = backwardModel;
        this.strategy = new GrowDiagonalFinalAndStrategy();
    }

    @Override
    public void load() throws AlignerException {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Void> forward = pool.submit(new LoadModelTask(forwardModel, true));
        Future<Void> backward = pool.submit(new LoadModelTask(backwardModel, true));

        try {
            forward.get();
            backward.get();
        } catch (InterruptedException e) {
            throw new AlignerException("Loading interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof AlignerException)
                throw (AlignerException) cause;
            else
                throw new AlignerException("Unexpected exception while loading models", cause);
        } finally {
            pool.shutdownNow();
        }
    }

    @Override
    public int[][] getAlignments(Sentence sentence, Sentence translation) throws AlignerException {
        int[][] forwardAlignments = forwardModel.getAlignments(sentence, translation);
        int[][] backwardAlignments = backwardModel.getAlignments(sentence, translation);

        int[][] alignments = strategy.symmetrize(forwardAlignments, backwardAlignments);
        if (logger.isDebugEnabled()) {
            logger.debug("Symmetrised alignments: " + Aligner.toString(alignments));
        }
        return AlignmentsInterpolator.interpolateAlignments(alignments, sentence.getWords().length, translation.getWords().length);
    }

    public void setSymmetrizationStrategy(SymmetrizationStrategy strategy) {
        if (strategy == null)
            throw new NullPointerException();

        this.strategy = strategy;
    }

    @Override
    public void close() throws IOException {
        try {
            forwardModel.close();
        } finally {
            backwardModel.close();
        }
    }

    private class LoadModelTask implements Callable<Void> {

        private final Aligner model;
        private final boolean forward;

        public LoadModelTask(Aligner model, boolean forward) {
            this.model = model;
            this.forward = forward;
        }

        @Override
        public Void call() throws AlignerException {
            logger.info(String.format("Loading %s model: %s", (forward ? "forward" : "backward"), model.getClass().getSimpleName()));

            long time = System.currentTimeMillis();
            model.load();
            long elapsed = System.currentTimeMillis() - time;

            logger.info(String.format("%s model loaded in %.1f", (forward ? "Forward" : "Backward"), elapsed / 1000.));

            return null;
        }
    }

}
