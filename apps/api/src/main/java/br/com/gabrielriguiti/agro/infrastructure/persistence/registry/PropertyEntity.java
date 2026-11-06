package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Linha da tabela {@code properties}; {@code producerId} nulo indica propriedade disponível. */
@Entity
@Table(name = "properties")
class PropertyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Column(name = "tax_id", nullable = false)
    String taxId;

    @Column(nullable = false)
    String address;

    @Column(name = "producer_id")
    Long producerId;

    @Column(name = "created_at", insertable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
