package com.dogonfire.gods.model;

/**
 * Where a god lives: where its first altar was built, or set with /g sethome. In gods.yml it is
 * {@code Home.World}, {@code Home.X}, {@code Home.Y}, {@code Home.Z}.
 */
public record GodHome(String world, double x, double y, double z)
{
}
