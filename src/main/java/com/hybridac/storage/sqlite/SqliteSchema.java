package com.hybridac.storage.sqlite;

import java.util.List;

public final class SqliteSchema {

    private SqliteSchema() {
    }

    public static List<String> statements() {
        return List.of(
                """
                CREATE TABLE IF NOT EXISTS stats (
                    key TEXT PRIMARY KEY,
                    value INTEGER NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS punish_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    score REAL NOT NULL,
                    reasons TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS recording_metadata (
                    session_id TEXT PRIMARY KEY,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    label TEXT NOT NULL,
                    world TEXT NOT NULL,
                    started_at INTEGER NOT NULL,
                    ended_at INTEGER NOT NULL,
                    hit_count INTEGER NOT NULL,
                    file_path TEXT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS ml_request_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    request_type TEXT NOT NULL,
                    payload_size INTEGER NOT NULL,
                    response_code INTEGER NOT NULL,
                    model_version TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS bot_evidence_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    bot_uuid TEXT NOT NULL,
                    bot_name TEXT NOT NULL,
                    score REAL NOT NULL,
                    detail TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS suspicion_summaries (
                    player_uuid TEXT PRIMARY KEY,
                    code_score REAL NOT NULL,
                    ml_score REAL NOT NULL,
                    bot_score REAL NOT NULL,
                    hybrid_score REAL NOT NULL,
                    evidence_count INTEGER NOT NULL,
                    reasons TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS model_metadata (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    last_model_version TEXT NOT NULL,
                    last_training_time TEXT NOT NULL
                )
                """
        );
    }
}
