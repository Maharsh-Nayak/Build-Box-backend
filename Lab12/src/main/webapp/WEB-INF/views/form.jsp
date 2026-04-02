<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>
<head>
    <title>User Registration</title>
</head>
<body>

<h2>User Registration Form</h2>

<form:form action="saveUser" method="post" modelAttribute="user">

    <table>

        <tr>
            <td>Name:</td>
            <td>
                <form:input path="name"/>
                <form:errors path="name" cssStyle="color:red"/>
            </td>
        </tr>

        <tr>
            <td>Age:</td>
            <td>
                <form:input path="age"/>
                <form:errors path="age" cssStyle="color:red"/>
            </td>
        </tr>

        <tr>
            <td>Gender:</td>
            <td>
                Male <form:radiobutton path="gender" value="M"/>
                Female <form:radiobutton path="gender" value="F"/>
                <form:errors path="gender" cssStyle="color:red"/>
            </td>
        </tr>

        <tr>
            <td>Email:</td>
            <td>
                <form:input path="email"/>
                <form:errors path="email" cssStyle="color:red"/>
            </td>
        </tr>

        <tr>
            <td></td>
            <td>
                <input type="submit" value="Submit"/>
            </td>
        </tr>

    </table>

</form:form>

</body>
</html>