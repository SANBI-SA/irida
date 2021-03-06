You may create a new user account in IRIDA if you have the **Manager** or **Administrator** system role.

After logging in to IRIDA, click on the **Users** menu, and select "Create User":

![Create user menu.]({{ site.baseurl }}/images/tutorials/common/users/create-user-menu.png)

Fill in the user details fields. All fields are required for a user account in IRIDA:

![User details form.]({{ site.baseurl }}/images/tutorials/common/users/user-details-form.png)

By default, a user will be issued a randomly-generated, one-time-use key to activate their account. The first time the user logs in, they will be required to change their password. You may manually enter a user password by unchecking "Require User Activation":

![Manual password entry.]({{ site.baseurl }}/images/tutorials/common/users/manual-user-password.png)

Passwords must have at least 1 upper-case character, 1 lower-case character, 1 number (0-9), and must be *at least* 8 characters long. If you manually set a password for a new user account, the user will not be required to change their password on first log in.

When you click "Create User", an e-mail will be sent to the user (to the e-mail address you entered) including the URL for IRIDA (as configured in the [Administrator Install Guide]({{ site.baseurl }}/administrator/web/#web-configuration)). If you did not manually set a password for the user, the e-mail will include a link to activate the user account:

![User welcome e-mail.]({{ site.baseurl }}/images/tutorials/common/users/user-welcome-email.png)

On successfully creating a new user account, you will see the user details that you entered:

![User details page.]({{ site.baseurl }}/images/tutorials/common/users/user-details-page.png)
