package com.example.gramolaRodrigo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.PlaybackState;

@Repository
public interface PlaybackStateRepository extends JpaRepository<PlaybackState, String> {

}
