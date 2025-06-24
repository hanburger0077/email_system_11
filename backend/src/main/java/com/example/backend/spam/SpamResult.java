package com.example.backend.spam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpamResult {
    private boolean isSpam;
    private String reason;
}