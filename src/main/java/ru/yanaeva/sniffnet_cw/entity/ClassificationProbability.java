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
@Table(name = "classification_probabilities")
public class ClassificationProbability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ClassificationRequest request;

    @Column(nullable = false, length = 100)
    private String className;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal probabilityValue;

    public Long getId() {
        return id;
    }

    public ClassificationRequest getRequest() {
        return request;
    }

    public void setRequest(ClassificationRequest request) {
        this.request = request;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public BigDecimal getProbabilityValue() {
        return probabilityValue;
    }

    public void setProbabilityValue(BigDecimal probabilityValue) {
        this.probabilityValue = probabilityValue;
    }
}
