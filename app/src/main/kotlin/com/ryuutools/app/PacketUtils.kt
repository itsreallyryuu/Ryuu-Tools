package com.ryuutools.app

object PacketUtils {

    fun computeIpChecksum(header: ByteArray): Int {
        var sum = 0
        var i = 0
        while (i < header.size) {
            val word = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    fun ipToBytes(ip: String): ByteArray {
        return ip.split(".").map { it.toInt().toByte() }.toByteArray()
    }

    /** Builds an IPv4 + UDP header (28 bytes) around [payload]. UDP checksum is
     * left as 0, which is valid per RFC 768 for IPv4 ("checksum not used"). */
    fun buildIpv4UdpPacket(
        srcIp: String, srcPort: Int,
        dstIp: String, dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)

        packet[0] = 0x45 // version 4, IHL 5 (20 bytes, no options)
        packet[1] = 0
        packet[2] = ((totalLength shr 8) and 0xFF).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = 0; packet[5] = 0
        packet[6] = 0x40.toByte(); packet[7] = 0 // Don't Fragment
        packet[8] = 64 // TTL
        packet[9] = 17 // UDP
        packet[10] = 0; packet[11] = 0 // checksum placeholder

        val srcBytes = ipToBytes(srcIp)
        val dstBytes = ipToBytes(dstIp)
        System.arraycopy(srcBytes, 0, packet, 12, 4)
        System.arraycopy(dstBytes, 0, packet, 16, 4)

        val checksum = computeIpChecksum(packet.copyOfRange(0, 20))
        packet[10] = ((checksum shr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()
        packet[24] = ((udpLength shr 8) and 0xFF).toByte()
        packet[25] = (udpLength and 0xFF).toByte()
        packet[26] = 0; packet[27] = 0

        System.arraycopy(payload, 0, packet, 28, payload.size)
        return packet
    }

    data class ParsedUdpPacket(
        val srcIp: String, val srcPort: Int,
        val dstIp: String, val dstPort: Int,
        val payload: ByteArray,
        val ipHeaderLength: Int
    )

    /** Parses a raw IPv4 packet from the TUN interface; returns null if it's not UDP. */
    fun parseIpv4Udp(buffer: ByteArray, length: Int): ParsedUdpPacket? {
        if (length < 20) return null
        val versionAndIhl = buffer[0].toInt() and 0xFF
        if (versionAndIhl shr 4 != 4) return null
        val ihl = (versionAndIhl and 0x0F) * 4
        if (ihl < 20 || length < ihl + 8) return null

        val protocol = buffer[9].toInt() and 0xFF
        if (protocol != 17) return null

        val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
        val dstIp = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"

        val udpStart = ihl
        val srcPort = ((buffer[udpStart].toInt() and 0xFF) shl 8) or (buffer[udpStart + 1].toInt() and 0xFF)
        val dstPort = ((buffer[udpStart + 2].toInt() and 0xFF) shl 8) or (buffer[udpStart + 3].toInt() and 0xFF)
        val udpLength = ((buffer[udpStart + 4].toInt() and 0xFF) shl 8) or (buffer[udpStart + 5].toInt() and 0xFF)

        val payloadStart = udpStart + 8
        val payloadLength = (udpLength - 8).coerceAtLeast(0).coerceAtMost(length - payloadStart)
        if (payloadLength <= 0 || payloadStart + payloadLength > length) return null

        val payload = buffer.copyOfRange(payloadStart, payloadStart + payloadLength)
        return ParsedUdpPacket(srcIp, srcPort, dstIp, dstPort, payload, ihl)
    }
}