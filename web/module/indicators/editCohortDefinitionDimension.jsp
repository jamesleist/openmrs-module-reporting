<%@ include file="../manage/localHeader.jsp"%>
<openmrs:require privilege="Manage Indicators" otherwise="/login.htm" redirect="/module/reporting/indicators/manageDimensions.form" />

<c:url value="/module/reporting/indicators/editCohortDefinitionDimension.form" var="pageUrl">
	<c:param name="uuid" value="${dimension.uuid}" />
</c:url>

<script type="text/javascript" charset="utf-8">
	$(document).ready(function() {

		// Redirect to listing page
		$('#cancel-button').click(function(event){
			window.location.href='<c:url value="/module/reporting/indicators/editCohortDefinitionDimension.form"/>';
		});

		$('#options-table').dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bFilter": false,
			"bSort": false,
			"bInfo": false,
			"bAutoWidth": false
		} );
		
	} );
</script>

<div id="page">
	<div id="container">
		<h1>Dimension Editor</h1>
		
		<c:choose>
			
			<c:when test="${dimension.uuid == null}">
				<b class="boxHeader">Create New Dimension</b>
				<div class="box">
					<openmrs:portlet url="baseMetadata" id="baseMetadata" moduleId="reporting" parameters="type=org.openmrs.module.indicator.dimension.CohortDefinitionDimension|size=380|mode=edit|dialog=false|cancelUrl=manageDimensions.form|successUrl=/module/reporting/indicators/editCohortDefinitionDimension.form?uuid=uuid" />
				</div>
			</c:when>
			
			<c:otherwise>
		
				<table style="font-size:small;">
					<tr>
						<td valign="top">
							<openmrs:portlet url="baseMetadata" id="baseMetadata" moduleId="reporting" parameters="type=${dimension.class.name}|uuid=${dimension.uuid}|size=380|label=Basic Details" />
							<br/>
							<openmrs:portlet url="parameter" id="newParameter" moduleId="reporting" parameters="type=${dimension.class.name}|uuid=${dimension.uuid}|label=Parameters|parentUrl=${pageUrl}" />
						</td>
						<td valign="top" width="100%">
							<b class="boxHeader">Options</b>
							<div class="box">
								<table id="options-table">
									<thead>
										<tr>
											<th>Key</th>
											<th>Cohort Definition</th>
											<th>&nbsp;</th>
										</tr>
									</thead>
									<tbody>
										<c:forEach var="cd" items="${dimension.cohortDefinitions}">
											<tr>
												<td>${cd.key}</td>
												<td>${cd.value.parameterizable.name}</td>
												<td>
													<a href="editCohortDefinitionDimensionRemoveOption.form?key=${cd.key}&uuid=${dimension.uuid}">
														<img src='<c:url value="/images/trash.gif"/>' border="0"/>
													</a>
												</td>
											</tr>
										</c:forEach>
									</tbody>
									<tfoot>
										<tr>
											<td colspan="3">
												<openmrs:portlet url="mappedProperty" id="newCd" moduleId="reporting" 
															 parameters="type=${dimension.class.name}|uuid=${dimension.uuid}|property=cohortDefinitions|mode=add|label=New Dimension Option" />
											</td>
										</tr>
									</tfoot>
								</table>
							</div>
						</td>
					</tr>
				</table>
				
			</c:otherwise>
			
		</c:choose>
	</div>
</div>
	
<%@ include file="/WEB-INF/template/footer.jsp"%>