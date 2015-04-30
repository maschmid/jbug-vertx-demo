package io.vertx.example;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class EntryParser {

	private Handler<JsonObject> docConsumer;
	private StringBuilder stringBuilder;
	private JsonObject entry;
	private String elementName;

	AsyncXMLStreamReader<AsyncByteArrayFeeder> asyncXmlParser;
	AsyncByteArrayFeeder feeder;

	int level = 0;

	public EntryParser(Handler<JsonObject> docConsumer) {
		this.docConsumer = docConsumer;

		asyncXmlParser = new InputFactoryImpl().createAsyncForByteArray();
		feeder = asyncXmlParser.getInputFeeder();
	}

	public void pushBuffer (Buffer buffer) {
		byte[] buff = buffer.getBytes();

		int type = 0;
		boolean bufferFed = false;

		try{

			waitForNextChunk: do {
				while((type = asyncXmlParser.next()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {		
					if (bufferFed) {
						break waitForNextChunk;
					}
					else {
						feeder.feedInput(buff, 0, buff.length);
						bufferFed = true;
					}
				}

				switch (type) {
				case XMLEvent.START_DOCUMENT:
					break;
				case XMLEvent.START_ELEMENT:

					String name = asyncXmlParser.getName().toString();
					switch(name) {
					case "entries":
						break;
					case "entry":
						entry = new JsonObject();
						break;
					default:
						elementName = name;
						stringBuilder = new StringBuilder();
					}

					break;
				case XMLEvent.CHARACTERS:
					stringBuilder.append(asyncXmlParser.getText());
					break; 
				case XMLEvent.END_ELEMENT:
					
					name = asyncXmlParser.getName().toString();
					switch(name) {
					case "entries":
						break;
					case "entry":
						docConsumer.handle(entry);
						break;
					default:
						entry.put(elementName, stringBuilder.toString());
					}
					break;
				case XMLEvent.END_DOCUMENT:
					break;
				default:
					break;
				}	
			} while(type != AsyncXMLStreamReader.END_DOCUMENT);
		}
		catch(XMLStreamException x) {
			x.printStackTrace();
		}
	}

	public void endOfInput() {

		try{

			feeder.endOfInput();
			int type = 0;

			endForReals: do {
				while((type = asyncXmlParser.next()) == AsyncXMLStreamReader.EVENT_INCOMPLETE) {   							    							
					System.out.println("Unexpected end of XML stream!");
					break endForReals;
				}				
			} while(type != AsyncXMLStreamReader.END_DOCUMENT);
		}
		catch(XMLStreamException x) {
			x.printStackTrace();
		}
	}
}
