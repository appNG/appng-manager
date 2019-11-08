package org.appng.application.manager.form;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class DemoClass implements Serializable {
	private Integer id;
	private String name;

	private void _writeObject(java.io.ObjectOutputStream out) throws IOException {
		log.error("writeObject " + getClass().getClassLoader() + " " + printStacktrace().getContextClassLoader());
		out.writeObject(id);
		out.writeObject(name);
	}

	private void _readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		log.error("readObject " + getClass().getClassLoader() + " " + printStacktrace().getContextClassLoader());
		id = (Integer) in.readObject();
		name = (String) in.readObject();
	}

	private void readObjectNoData() throws ObjectStreamException {

	}

	private Thread printStacktrace() {
		StringBuilder sb = new StringBuilder();
		Thread currentThread = Thread.currentThread();
		StackTraceElement[] stackTrace = currentThread.getStackTrace();
		for (StackTraceElement stackTraceElement : stackTrace) {
			sb.append(stackTraceElement + "\r\n");
		}
		log.error(sb.toString());
		return currentThread;
	}
}
