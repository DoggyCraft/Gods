package com.dogonfire.gods.model;

/**
 * The holy land a god is attacking (a priest started the attack with /g startattack): the block it was started at and
 * the land's region hash. In gods.yml it is {@code ContestedLand.World/X/Y/Z/Hash}. {@code HolyLandService} owns the
 * attack itself (kills, time); this is the attacker's side.
 */
public record ContestedLand(String world, int x, int y, int z, long hash)
{
}
