package com.dogonfire.gods.model;

/**
 * The holy land a god is attacking (a priest started the attack with /g startattack): the block it
 * was started at, in the attacked land. In gods.yml it is {@code ContestedLand.World/X/Y/Z}. {@code
 * HolyLandService} owns the attack itself (kills, time, on the land); this is the attacker's side.
 *
 * @param hash the region hash Gods wrote before holy land was kept by chunks ({@code
 *     ContestedLand.Hash}), written back while that attack lasts; null for newer attacks. Not used:
 *     the land is the one at the block.
 */
public record ContestedLand(String world, int x, int y, int z, Long hash) {}
