package com.aiinterview.rubric.dto;

/**
 * versionNumber는 클라이언트가 정하지 않는다 — 서비스가 회사별 max+1로 매긴다.
 * (CLAUDE.md 원칙 3: 루브릭은 덮어쓰지 않고 버전이 오른다.)
 */
public record RubricVersionCreateRequest(String changelog) {}
