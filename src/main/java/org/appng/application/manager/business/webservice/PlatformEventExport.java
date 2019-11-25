package org.appng.application.manager.business.webservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.appng.api.AttachmentWebservice;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.business.PlatformEvents;
import org.appng.application.manager.business.PlatformEvents.EventFilter;
import org.appng.application.manager.service.PlatformEventService;
import org.appng.core.domain.PlatformEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import com.google.common.net.MediaType;

@Service
@RequestScope
public class PlatformEventExport implements AttachmentWebservice {

	private static final FastDateFormat FILE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm");
	private static final FastDateFormat CELL_FORMAT = FastDateFormat.getInstance("yy/MM/dd HH:mm:ss");
	private PlatformEventService platformEventEventService;
	private MessageSource messageSource;

	@Autowired
	public PlatformEventExport(PlatformEventService platformEventEventService, MessageSource messageSource) {
		this.platformEventEventService = platformEventEventService;
		this.messageSource = messageSource;
	}

	public byte[] processRequest(Site site, Application application, Environment environment, Request request)
			throws BusinessException {
		if (environment.isSubjectAuthenticated() && request.getPermissionProcessor().hasPermission("platform.events")) {

			EventFilter filter = environment.getAttribute(Scope.SESSION, PlatformEvents.EVENT_FILTER);
			List<PlatformEvent> events = platformEventEventService.getEvents(filter);
			try {
				try (ByteArrayOutputStream out = getEventReport(events, messageSource)) {
					return out.toByteArray();
				}
			} catch (IOException e) {
				throw new BusinessException(e);
			}
		}
		throw new BusinessException("Not allowed!");
	}

	public static ByteArrayOutputStream getEventReport(List<PlatformEvent> events, MessageSource messageSource)
			throws IOException {
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = wb.createSheet();
		int row = 0, col = 0;

		Font headerFont = wb.createFont();
		headerFont.setBold(true);
		CellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(row++);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.DATE);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.TYPE);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.EVENT);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.APPLICATION);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.CONTEXT);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.USER);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.HOST);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.HOST_NAME);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.REQUEST);
		createHeader(header, col++, headerStyle, messageSource, MessageConstants.SESSION);

		for (PlatformEvent e : events) {
			col = 0;
			Row item = sheet.createRow(row++);
			item.createCell(col++).setCellValue(CELL_FORMAT.format(e.getCreated()));
			item.createCell(col++).setCellValue(e.getType().name());
			item.createCell(col++).setCellValue(e.getEvent());
			item.createCell(col++).setCellValue(e.getApplication());
			item.createCell(col++).setCellValue(e.getContext());
			item.createCell(col++).setCellValue(e.getUser());
			item.createCell(col++).setCellValue(e.getOrigin());
			item.createCell(col++).setCellValue(e.getHostName());
			item.createCell(col++).setCellValue(e.getRequestId());
			item.createCell(col++).setCellValue(e.getSessionId());
		}
		for (int i = 0; i < 10; i++) {
			if (i == 2) {
				sheet.setColumnWidth(i, sheet.getColumnWidth(i) * 4);
			} else {
				sheet.autoSizeColumn(i);
			}
		}
		sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, 9));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		wb.write(out);
		wb.close();
		out.close();
		return out;
	}

	private static void createHeader(Row header, int col, CellStyle headerStyle, MessageSource messageSource,
			String key) {
		Cell cell = header.createCell(col);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(getMessage(messageSource, key));
	}

	private static String getMessage(MessageSource messageSource, String key) {
		return messageSource.getMessage(key, new Object[0], Locale.ENGLISH);
	}

	public String getContentType() {
		return MediaType.OPENDOCUMENT_SPREADSHEET.toString();
	}

	public String getFileName() {
		return getAttachmentName();
	}

	public static String getAttachmentName() {
		return FILE_FORMAT.format(System.currentTimeMillis()) + "-platformevents.xlsx";
	}

	public boolean isAttachment() {
		return true;
	}

}