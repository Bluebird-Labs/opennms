/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.provision.support.codec;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.opennms.netmgt.provision.detector.simple.response.LineOrientedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>TcpLineDecoder class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class TcpLineDecoder extends CumulativeProtocolDecoder {
    
    private static final Logger LOG = LoggerFactory.getLogger(TcpLineDecoder.class);
    public final static String NO_MESSAGES_RECEIVED = "___OPENNMS_NO_TCP_BANNER_RECEIVED___";

    private Charset m_charset;
    
    /**
     * <p>Constructor for TcpLineDecoder.</p>
     *
     * @param charset a {@link java.nio.charset.Charset} object.
     */
    public TcpLineDecoder(final Charset charset) {
        setCharset(charset);
    }   
    
    private void setCharset(final Charset charset) {
        m_charset = charset;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doDecode(final IoSession session, final IoBuffer in, final ProtocolDecoderOutput out) throws Exception {
     // Remember the initial position.
        final int start = in.position();

        // Now find the first CRLF in the buffer.
        byte previous = 0;
        while (in.hasRemaining()) {
            
            final byte current = in.get();
            
            if (previous == '\r' && current == '\n') {
                // Remember the current position and limit.
                final int position = in.position();
                final int limit = in.limit();
                try {
                    in.position(start);
                    in.limit(position);
                    // The bytes between in.position() and in.limit()
                    // now contain a full CRLF terminated line.
                    LOG.debug("slice: {}", in.slice());
                    out.write(parseCommand(in.slice()));
                } finally {
                    // Set the position to point right after the
                    // detected line and set the limit to the old
                    // one.
                    in.position(position);
                    in.limit(limit);
                }
                
                // Decoded one line; CumulativeProtocolDecoder will
                // call me again until I return false. So just
                // return true until there are no more lines in the
                // buffer.
                return true;
                
                }
            
            previous = current;
        }
        
        // Could not find CRLF in the buffer. Reset the initial
        // position to the one we recorded above.
        in.position(start);
        
        out.write(parseCommand(in.slice()));
        
        return false;
    }
    
    /**
     * <p>parseCommand</p>
     *
     * @param in a {@link org.apache.mina.core.buffer.IoBuffer} object.
     * @return a {@link java.lang.Object} object.
     */
    protected Object parseCommand(final IoBuffer in) {
        String outputStr = null;
        try {
            outputStr = in.getString(getCharset().newDecoder());
        } catch(final CharacterCodingException e) {
            outputStr = convertToString(in);
        }
        
        return new LineOrientedResponse(outputStr);
    }
    
    private String convertToString(final IoBuffer in) {
        final StringBuilder sb = new StringBuilder();
        while(in.hasRemaining()){
            byte current = in.get();
            sb.append((char)current);
        }
        return sb.toString();
    }

    /**
     * <p>getCharset</p>
     *
     * @return a {@link java.nio.charset.Charset} object.
     */
    public Charset getCharset() {
        return m_charset;
    }
    
    @Override
    public void finishDecode(final IoSession session, final ProtocolDecoderOutput out) {
        if(session.getReadMessages() == 0) {
            out.write(new LineOrientedResponse(NO_MESSAGES_RECEIVED));
        }
        
    }

}
