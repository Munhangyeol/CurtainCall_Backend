package com.example.curtaincall;

import com.example.curtaincall.domain.RecentCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentCallLogRepository extends JpaRepository<RecentCallLog,Long> {
    Optional<List<RecentCallLog>> findByPhoneNumber(String phoneNumber);
}
