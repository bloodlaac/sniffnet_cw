package ru.yanaeva.sniffnet_cw.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "training_configs")
public class TrainingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer epochsNum;

    @Column(nullable = false)
    private Integer batchSize;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal learningRate;

    @Column(nullable = false, length = 100)
    private String optimizer;

    @Column(nullable = false, length = 100)
    private String lossFunction;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal validationSplit;

    private Integer layersNum;

    private Integer neuronsNum;

    public Long getId() {
        return id;
    }

    public Integer getEpochsNum() {
        return epochsNum;
    }

    public void setEpochsNum(Integer epochsNum) {
        this.epochsNum = epochsNum;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public BigDecimal getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(BigDecimal learningRate) {
        this.learningRate = learningRate;
    }

    public String getOptimizer() {
        return optimizer;
    }

    public void setOptimizer(String optimizer) {
        this.optimizer = optimizer;
    }

    public String getLossFunction() {
        return lossFunction;
    }

    public void setLossFunction(String lossFunction) {
        this.lossFunction = lossFunction;
    }

    public BigDecimal getValidationSplit() {
        return validationSplit;
    }

    public void setValidationSplit(BigDecimal validationSplit) {
        this.validationSplit = validationSplit;
    }

    public Integer getLayersNum() {
        return layersNum;
    }

    public void setLayersNum(Integer layersNum) {
        this.layersNum = layersNum;
    }

    public Integer getNeuronsNum() {
        return neuronsNum;
    }

    public void setNeuronsNum(Integer neuronsNum) {
        this.neuronsNum = neuronsNum;
    }
}
