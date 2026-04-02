<%@ page contentType="text/html;charset=UTF-8" %>

<html>
<head>
    <title>User Information</title>
</head>
<body>

<h2>User Details</h2>

<table border="1" cellpadding="10">

    <tr>
        <td><b>Name</b></td>
        <td>${user.name}</td>
    </tr>

    <tr>
        <td><b>Age</b></td>
        <td>${user.age}</td>
    </tr>

    <tr>
        <td><b>Gender</b></td>
        <td>${user.gender}</td>
    </tr>

    <tr>
        <td><b>Email</b></td>
        <td>${user.email}</td>
    </tr>

</table>

<br>

<form action="editUser" method="get">
    <button type="submit">Edit</button>
</form>

</body>
</html>