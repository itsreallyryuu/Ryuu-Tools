package com.ryuutools.app

object DnsUtils {

    /** Extracts the queried domain name from a raw DNS query's first question. */
    fun extractQueryDomain(dns: ByteArray): String? {
        if (dns.size < 12) return null
        var pos = 12
        val labels = mutableListOf<String>()
        try {
            while (pos < dns.size) {
                val len = dns[pos].toInt() and 0xFF
                if (len == 0) { pos++; break }
                pos++
                if (pos + len > dns.size) return null
                labels.add(String(dns, pos, len, Charsets.US_ASCII))
                pos += len
            }
        } catch (e: Exception) {
            return null
        }
        if (labels.isEmpty()) return null
        return labels.joinToString(".")
    }

    /** Flips a query into an NXDOMAIN response, keeping the same transaction ID
     * and question section so the requesting app accepts it as a valid reply. */
    fun buildNxDomainResponse(originalQuery: ByteArray): ByteArray {
        val response = originalQuery.copyOf()
        if (response.size < 4) return response
        response[2] = (response[2].toInt() or 0x80).toByte() // QR = 1 (response)
        response[3] = ((response[3].toInt() and 0xF0) or 0x03).toByte() // RCODE = 3 (NXDOMAIN)
        return response
    }
}