package ru.yanaeva.sniffnet_cw.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "metrics")
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private TrainingConfig config;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal trainAccuracy;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal trainLoss;

    @Column(precision = 5, scale = 4)
    private BigDecimal validationAccuracy;

    @Column(precision = 5, scale = 4)
    private BigDecimal validationLoss;

    @Column(columnDefinition = "text")
    private String detailsJson;

    public Long getId() {
        return id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public TrainingConfig getConfig() {
        return config;
    }

    public void setConfig(TrainingConfig config) {
        this.config = config;
    }

    public BigDecimal getTrainAccuracy() {
        return trainAccuracy;
    }

    public void setTrainAccuracy(BigDecimal trainAccuracy) {
        this.trainAccuracy = trainAccuracy;
    }

    public BigDecimal getTrainLoss() {
        return trainLoss;
    }

    public void setTrainLoss(BigDecimal trainLoss) {
        this.trainLoss = trainLoss;
    }

    public BigDecimal getValidationAccuracy() {
        return validationAccuracy;
    }

    public void setValidationAccuracy(BigDecimal validationAccuracy) {
        this.validationAccuracy = validationAccuracy;
    }

    public BigDecimal getValidationLoss() {
        return validationLoss;
    }

    public void setValidationLoss(BigDecimal validationLoss) {
        this.validationLoss = validationLoss;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }
}
