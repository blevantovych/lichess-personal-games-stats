package com.lichess.insights;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Integer> {

	Page<Game> findByWhiteElo(String whiteElo, Pageable pageable);

}
