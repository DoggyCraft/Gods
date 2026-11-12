package com.dogonfire.gods.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class HelpPagesTest
{
	private static final List<Integer> COMMANDS = IntStream.rangeClosed(1, 19).boxed().toList();

	@Test
	void pagesHoldEightCommandsEach()
	{
		assertEquals(3, HelpPages.pageCount(19, HelpPages.PER_PAGE));
		assertEquals(2, HelpPages.pageCount(16, HelpPages.PER_PAGE));
		assertEquals(1, HelpPages.pageCount(8, HelpPages.PER_PAGE));
		assertEquals(1, HelpPages.pageCount(0, HelpPages.PER_PAGE));
		assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), HelpPages.page(COMMANDS, 1, HelpPages.PER_PAGE));
		assertEquals(List.of(9, 10, 11, 12, 13, 14, 15, 16), HelpPages.page(COMMANDS, 2, HelpPages.PER_PAGE));
		assertEquals(List.of(17, 18, 19), HelpPages.page(COMMANDS, 3, HelpPages.PER_PAGE));
	}

	@Test
	void pagesThatArentThereAreEmpty()
	{
		assertEquals(List.of(), HelpPages.page(COMMANDS, 4, HelpPages.PER_PAGE));
		assertEquals(List.of(), HelpPages.page(COMMANDS, 0, HelpPages.PER_PAGE));
		assertEquals(List.of(), HelpPages.page(List.of(), 1, HelpPages.PER_PAGE));
	}

	@Test
	void onlyNumbersArePages()
	{
		assertEquals(2, HelpPages.parsePage("2"));
		assertEquals(10, HelpPages.parsePage("10"));
		assertEquals(-1, HelpPages.parsePage("altar"));
		assertEquals(-1, HelpPages.parsePage("-1"));
		assertEquals(-1, HelpPages.parsePage(""));
		assertEquals(-1, HelpPages.parsePage("99999999999"));
		assertEquals(List.of("1", "2", "3"), HelpPages.pageNumbers(3));
	}
}
