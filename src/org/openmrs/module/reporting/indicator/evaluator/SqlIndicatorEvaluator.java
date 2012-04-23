package org.openmrs.module.reporting.indicator.evaluator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.module.reporting.IllegalDatabaseAccessException;
import org.openmrs.module.reporting.ReportingException;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterException;
import org.openmrs.module.reporting.indicator.Indicator;
import org.openmrs.module.reporting.indicator.SimpleIndicatorResult;
import org.openmrs.module.reporting.indicator.SqlIndicator;
import org.openmrs.module.reporting.indicator.SqlIndicator.QueryString;
import org.openmrs.module.reporting.report.util.SqlUtils;
import org.openmrs.util.DatabaseUpdater;


@Handler(supports={SqlIndicator.class})
public class SqlIndicatorEvaluator implements IndicatorEvaluator {

	protected Log log = LogFactory.getLog(this.getClass());	
	

	 public SimpleIndicatorResult evaluate(Indicator indicator, EvaluationContext context) throws EvaluationException {

		 SqlIndicator sqlIndicator = (SqlIndicator) indicator;

		 SimpleIndicatorResult result = new SimpleIndicatorResult();
		 result.setIndicator(indicator);
		 result.setContext(context);
		 Mapped<QueryString> q = sqlIndicator.getSql();
		 String sql = new String(q.getParameterizable().getSql());

		 //TODO:  EVERYTHING FROM HERE DOWN IS A HACK PENDING REPORT-380
		 //this validates, prepares, and runs the query, and set the numerator and denominator on the result object
		 validateQuery(sql, context.getParameterValues());
		 executeSql(sql, context.getParameterValues(), result, "numerator");
		 if (sqlIndicator.getDenominatorSql() != null)
			 executeSql(new String(sqlIndicator.getDenominatorSql().getParameterizable().getSql()), context.getParameterValues(), result, "denominator");	 
		 return result;
	 }
	 

	 
	 /**
	  *   EVERYTHING BELOW CAN BE REPLACED, PENDING REPORT-380
	  * 
	  */
	 
	 private void executeSql(String sql, Map<String, Object> paramMap, SimpleIndicatorResult result, String resultType) throws EvaluationException {
			Connection connection = null;
			try {
				connection = DatabaseUpdater.getConnection();
				ResultSet resultSet = null;
				
				PreparedStatement statement = SqlUtils.prepareStatement(connection, sql.toString(), paramMap);
				boolean queryResult = statement.execute();

				if (!queryResult) {
					throw new EvaluationException("Unable to evaluate sql query");
				}
				resultSet = statement.getResultSet();
				while (resultSet.next()) {
					if (resultType.equals("numerator")){
						if (resultSet.getObject(1) == null)
							continue;						
						else if (Math.rint(resultSet.getDouble(1)) == resultSet.getDouble(1))  //if not decimal
							result.setNumeratorResult(resultSet.getInt(1));
						else
							result.setNumeratorResult(BigDecimal.valueOf(resultSet.getDouble(1)));
					}	
					else if (resultType.equals("denominator") && !resultSet.wasNull())
						result.setDenominatorResult(resultSet.getInt(1));
				}
			}
			catch (IllegalDatabaseAccessException ie) {
				throw ie;
			}
			catch (Exception e) {
				throw new EvaluationException("Unable to evaluate sql query", e);
			}
			finally {
				try {
					if (connection != null) {
						connection.close();
					}
				}
				catch (Exception e) {
					log.error("Error while closing connection", e);
				}
			}
	 }
	 
	 /**
	  * TODO:  SHOULD BE REPLACED BY REPORT-380
	  */
	 private void validateQuery(String sql, Map<String, Object> paramMap){
		 if (sql == null || sql.equals("")) 
				throw new ReportingException("SQL query string is required");
			if (!SqlUtils.isSelectQuery(sql)) {
				throw new IllegalDatabaseAccessException();
			}
	    	List<Parameter> parameters = getNamedParameters(sql);    	
	    	for (Parameter parameter : parameters) { 
	    		Object parameterValue = paramMap.get(parameter.getName());
	    		if (parameterValue == null) 
	    			throw new ParameterException("Must specify a value for the parameter [" +  parameter.getName() + "]");    		
	    	}	
	 }
	 
	 /**
	  * TODO:  SHOULD BE REPLACED BY REPORT-380
	  */
	 private List<Parameter> getNamedParameters(String sqlQuery) {
		List<Parameter> parameters = new ArrayList<Parameter>();

		// TODO Need to move regex code into a utility method 
		Pattern pattern = Pattern.compile("\\:\\w+\\b");
		Matcher matcher = pattern.matcher(sqlQuery);

		while (matcher.find()) {			
			// Index is 1 because we need to strip off the colon (":")
			String parameterName = matcher.group().substring(1);			
			Parameter parameter = new Parameter();			
			parameter.setName(parameterName);
			parameter.setLabel(parameterName);
			if (parameterName.toLowerCase().contains("date")) {
				parameter.setType(Date.class);
			}
			else {
				parameter.setType(String.class);
			}
			parameters.add(parameter);
		}		
		return parameters;
	 }
	
}