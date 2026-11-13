package com.dogonfire.gods.command;

import java.util.List;

/** How /g help splits the commands into pages, without a server so it can be tested */
public final class HelpPages {
  /** Commands on one page of /g help */
  public static final int PER_PAGE = 8;

  private HelpPages() {}

  /**
   * How many pages {@code items} entries fill (at least one, so an empty list still has a page to
   * show)
   */
  public static int pageCount(int items, int perPage) {
    return Math.max(1, (items + perPage - 1) / perPage);
  }

  /** The entries on page {@code page} (from 1); empty for a page that isn't there */
  public static <T> List<T> page(List<T> items, int page, int perPage) {
    int from = (page - 1) * perPage;
    if (page < 1 || from >= items.size()) {
      return List.of();
    }
    return List.copyOf(items.subList(from, Math.min(from + perPage, items.size())));
  }

  /** The page number {@code argument} names (digits only), or -1 if it is no number */
  public static int parsePage(String argument) {
    if (argument.isEmpty()
        || argument.length() > 6
        || !argument.chars().allMatch(Character::isDigit)) {
      return -1;
    }
    return Integer.parseInt(argument);
  }

  /** The page numbers, as tab completion offers them */
  public static List<String> pageNumbers(int pages) {
    return java.util.stream.IntStream.rangeClosed(1, pages).mapToObj(Integer::toString).toList();
  }
}
