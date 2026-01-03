package com.example.gramolaRodrigo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.SongPrice;

@Repository
public interface SongPriceRepository extends JpaRepository<SongPrice, Long> {
    Optional<SongPrice> findFirstByActiveTrue();
}
