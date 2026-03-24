package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class MockTrainingAdapter implements TrainingAdapter {

    @Override
    public TrainingResult startTraining(TrainingStartRequest request) {
        boolean success = request.epochsNum() % 5 != 0;
        if (!success) {
            return new TrainingResult(
                false,
                "failed-model-" + request.experimentId(),
                null,
                0,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                null
            );
        }

        BigDecimal trainAccuracy = BigDecimal.valueOf(
            Math.min(0.99, 0.70 + request.validationSplit().doubleValue() * 0.20
            + request.epochsNum() * 0.005));
        BigDecimal trainLoss = BigDecimal.valueOf(Math.max(0.05, 0.80 - request.epochsNum() * 0.02));
        BigDecimal validationAccuracy = trainAccuracy.subtract(BigDecimal.valueOf(0.03));
        BigDecimal validationLoss = trainLoss.add(BigDecimal.valueOf(0.04));

        return new TrainingResult(
            true,
            "sniffnet-model-" + request.experimentId(),
            "mock://models/" + request.experimentId(),
            120000 + request.epochsNum() * 2000,
            30L + request.epochsNum() * 3L,
            trainAccuracy,
            trainLoss,
            validationAccuracy.max(BigDecimal.ZERO),
            validationLoss,
            "mock://reports/" + request.experimentId() + ".json"
        );
    }
}
