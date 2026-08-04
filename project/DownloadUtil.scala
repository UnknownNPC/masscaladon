import sbt.*

import java.net.URI

object DownloadUtil {

  def download(urlStr: String, fileName: String, destDir: File, logger: Logger): File = {
    val destFile = destDir / fileName

    if (destFile.exists()) {
      logger.info(s"File $fileName already exists at $destFile. Skipping download.")
      destFile
    } else {
      val url = new URI(urlStr).toURL

      logger.info(s"Downloading file $fileName: from $urlStr to $destFile")

      val input = url.openStream()
      try {
        IO.transfer(input, destFile)
      } finally {
        input.close()
      }

      logger.info("Download is done")
      destFile
    }
  }

}
