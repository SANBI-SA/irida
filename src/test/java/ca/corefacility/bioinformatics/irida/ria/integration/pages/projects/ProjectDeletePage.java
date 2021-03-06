package ca.corefacility.bioinformatics.irida.ria.integration.pages.projects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import ca.corefacility.bioinformatics.irida.ria.integration.pages.AbstractPage;

/**
 * Page for project delete settings page
 */
public class ProjectDeletePage extends AbstractPage {

	@FindBy(id = "confirm-deletion")
	private WebElement confirmCheckbox;

	@FindBy(id = "submit-delete")
	private WebElement submitDelete;

	public ProjectDeletePage(WebDriver driver) {
		super(driver);
	}

	public static ProjectDeletePage goTo(WebDriver driver, Long projectId) {
		get(driver, "projects/" + projectId + "/settings/delete");
		waitForTime(500);
		return PageFactory.initElements(driver, ProjectDeletePage.class);
	}

	public void deleteProject() {
		confirmCheckbox.click();
		submitDelete.click();
	}

}
