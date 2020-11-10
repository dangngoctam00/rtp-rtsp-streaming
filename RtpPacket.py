import sys
from time import time
HEADER_SIZE = 12

class RtpPacket:	
	header = bytearray(HEADER_SIZE)
	
	def __init__(self):
		pass
	def encode(self, version, padding, extension, cc, seqnum, marker, pt, ssrc, payload):
		"""Encode the RTP packet with header fields and payload."""
		timestamp = int(time())
		header = bytearray(HEADER_SIZE)
		#--------------
		# TO COMPLETE
		#--------------
		# Fill the header bytearray with RTP header fields
		
		# header[0] = ...
		# ...
		header[0] = (header[0] | version << 6) & 0b11000000
		header[0] = (header[0] | padding << 5)
		header[0] = (header[0] | extension << 4)
		header[0] = (header[0] | cc & 0b1111)
		header[1] = (header[1] | marker << 7) & 0b10000000
		header[1] = (header[1] | pt & 0b01111111)
		header[2] = ((seqnum & 0xFF00 )>> 8)
		header[3] = (seqnum & 0xFF)
		header[4] = ((timestamp & 0xFF000000) >> 24)
		header[5] = ((timestamp & 0xFF0000) >> 16)
		header[6] = ((timestamp & 0xFF00) >> 8)
		header[7] = (timestamp & 0xFF)
		header[8] = ((ssrc & 0xFF000000) >> 24)
		header[9] = ((ssrc & 0xFF0000) >> 16)
		header[10] = ((ssrc & 0xFF00) >> 8)
		header[11] = (ssrc & 0xFF)

		self.header = header
		# Get the payload from the argument
		self.payload = payload
		
	def getPacket(self):
		"""Return RTP packet."""
		return self.header + self.payload