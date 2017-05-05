package ee.ttu.idk0071.sentiment.services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ee.ttu.idk0071.sentiment.model.ErrorLog;
import ee.ttu.idk0071.sentiment.repository.ErrorLogRepository;

@Service
public class LogService {
	@Autowired
	private ErrorLogRepository errorLogRepository;

	public void logError(Throwable t, Class<?> source) {
		// TODO add optional properties
		Logger logger = Logger.getLogger(source);
		logger.log(Level.ERROR, t);
		StringWriter stackWriter = new StringWriter();
		PrintWriter stackPrintWriter = new PrintWriter(stackWriter);
		t.printStackTrace(stackPrintWriter);
		
		ErrorLog errorLog = new ErrorLog();
		
		errorLog.setDate(new Date());
		errorLog.setSource(source.getName());
		errorLog.setMessage(t.getMessage());
		errorLog.setStackTrace(stackWriter.toString());
		
		errorLogRepository.save(errorLog);
	};

}