package ru.yanaeva.sniffnet_cw.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "models")
public class ModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private TrainingConfig config;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    private Integer paramsNum;

    @Column(nullable = false)
    private Long trainingTimeSeconds = 0L;

    @Column(nullable = false)
    private Boolean availableForInference = false;

    @Column(length = 255)
    private String weightsPath;

    @Column(unique = true)
    private Long externalModelId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public Integer getParamsNum() {
        return paramsNum;
    }

    public void setParamsNum(Integer paramsNum) {
        this.paramsNum = paramsNum;
    }

    public Long getTrainingTimeSeconds() {
        return trainingTimeSeconds;
    }

    public void setTrainingTimeSeconds(Long trainingTimeSeconds) {
        this.trainingTimeSeconds = trainingTimeSeconds;
    }

    public Boolean getAvailableForInference() {
        return availableForInference;
    }

    public void setAvailableForInference(Boolean availableForInference) {
        this.availableForInference = availableForInference;
    }

    public String getWeightsPath() {
        return weightsPath;
    }

    public void setWeightsPath(String weightsPath) {
        this.weightsPath = weightsPath;
    }

    public Long getExternalModelId() {
        return externalModelId;
    }

    public void setExternalModelId(Long externalModelId) {
        this.externalModelId = externalModelId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
