package org.cafienne.util

import org.slf4j.LoggerFactory

import java.lang.management.{ManagementFactory, ThreadMXBean}

object DeadLockThreadLogger {
  private val logger = LoggerFactory.getLogger(classOf[DeadLockThreadLogger.type])

  def reportDeadLocks(): Unit = {
    logger.error("report deadlocks")
    val dumpThread = new Thread(new Runnable() {
      override def run(): Unit = {
        val bean: ThreadMXBean = ManagementFactory.getThreadMXBean
        val threadIds: Array[Long] = bean.findDeadlockedThreads // Returns null if no threads are deadlocked.

        if (threadIds != null) {
          val infos = bean.getThreadInfo(threadIds)
          for (info <- infos) {
            val stack = info.getStackTrace
            logger.error("Thread issue " + stack.toString)
          }
        }
      }
    })
    dumpThread.start()
  }

}
