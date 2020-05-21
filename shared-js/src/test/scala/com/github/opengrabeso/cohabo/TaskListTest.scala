package com.github.opengrabeso.cohabo

import TaskList.Progress

class TaskListTest extends org.scalatest.funsuite.AnyFunSuite {
  test("Parse empty text") {
    assert(TaskList.progress("") == Progress(0, 0))
  }
  test("Parse single task") {
    assert(TaskList.progress("- [ ]") == Progress(0, 1))
    assert(TaskList.progress("- [x]") == Progress(1, 1))
    assert(TaskList.progress("- [X]") == Progress(1, 1))
  }

  test("Ignore non-tasks") {
    assert(TaskList.progress("- [a]") == Progress(0, 0))
    assert(TaskList.progress("- [  ]") == Progress(0, 0))
    assert(TaskList.progress("- [ x]") == Progress(0, 0))
    assert(TaskList.progress("- [xx]") == Progress(0, 0))
  }

  test("Parse multiple tasks with different list styles") {
    val text =
      """
         - [ ] item
         * [ ] item
         1. [ ] item
         5. [ ] item
           - [x] item
           * [x] item
           1. [x] item
           5. [x] item
        """
    assert(TaskList.progress(text) == Progress(4, 8))
  }
}
