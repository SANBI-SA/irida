package ca.corefacility.bioinformatics.irida.ria.integration.announcements;

import ca.corefacility.bioinformatics.irida.ria.integration.AbstractIridaUIITChromeDriver;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.LoginPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.announcements.*;

import com.github.springtestdbunit.annotation.DatabaseSetup;

import com.github.springtestdbunit.annotation.DatabaseTearDown;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration test to ensure the Announcement Control page works
 */
@DatabaseSetup("/ca/corefacility/bioinformatics/irida/ria/web/announcements/AnnouncementPageIT.xml")
@DatabaseTearDown("/ca/corefacility/bioinformatics/irida/test/integration/TableReset.xml")
public class AnnouncementPageIT extends AbstractIridaUIITChromeDriver{

    //Page objects
    private AnnouncementControlPage controlPage;
    private AnnouncementCreatePage createPage;
    private AnnouncementReadPage readPage;
    private AnnouncementDetailPage detailPage;
    private AnnouncementDashboardPage dashboardPage;

    @Before
    public void setUpTest() {
        LoginPage.loginAsAdmin(driver());
        controlPage = new AnnouncementControlPage(driver());
        createPage = new AnnouncementCreatePage(driver());
        readPage = new AnnouncementReadPage(driver());
        detailPage = new AnnouncementDetailPage(driver());
        dashboardPage = new AnnouncementDashboardPage(driver());
    }

    @Test
    public void testConfirmTablePopulatedByAnnouncements() {
        controlPage.goTo();
        assertEquals("Announcement table should be populated by 7 announcements", 7, controlPage.announcementTableSize());
    }

    @Test
    public void testSortAnnouncementsByContent() {
        controlPage.goTo();
        controlPage.clickMessageHeader();

        List<WebElement> announcements = controlPage.getVisibleAnnouncementsContent();

        assertTrue("List of announcements is not sorted correctly", checkSortedAscending(announcements));

        controlPage.clickMessageHeader();

        announcements = controlPage.getVisibleAnnouncementsContent();

        assertTrue("List of announcements is not sorted correctly", checkSortedDescending(announcements));
    }

    @Test
    public void testSubmitNewAnnouncement() {
        String message = "This is a great announcement";

        controlPage.goTo();

        int numAnnouncementsBefore = controlPage.getVisibleAnnouncementsContent().size();

        controlPage.clickCreateNewAnnouncementButton();
        createPage.enterMessage(message);
        controlPage.waitForDatatableAjax();

        //check the announcements on the page to make sure that the message is one of the announcements
        List<WebElement> announcements = controlPage.getVisibleAnnouncementsContent();
        List<String> content = announcements.stream().map(c -> c.getText()).collect(Collectors.toList());

        assertTrue("Unexpected announcement content.", content.get(0).equals(message));
        assertEquals("Unexpected number of announcements visible", numAnnouncementsBefore + 1, announcements.size());
    }

    @Test
    public void testCheckDetailsPage() {
        controlPage.goTo();

        List<WebElement> content = controlPage.getVisibleAnnouncementsContent();

        String announcement1 = content.get(1).getText();
        String announcement2 = content.get(2).getText();
        String announcement3 = content.get(3).getText();

        controlPage.clickDetailsButton(1);
        assertTrue("Announcement content doesn't match expected", announcement1.equals(detailPage.getInputText()));

        detailPage.clickCancelButton();

        controlPage.clickDetailsButton(2);
        assertTrue("Announcement content doesn't match expected", announcement2.equals(detailPage.getInputText()));

        detailPage.clickCancelButton();

        controlPage.clickDetailsButton(3);
        assertTrue("Announcement content doesn't match expected", announcement3.equals(detailPage.getInputText()));
    }

    @Test
    public void testUpdateAnnouncement() {
        String newMessage = "Updated!!!";

        controlPage.goTo();
        controlPage.clickDetailsButton(4);
        detailPage.enterMessage(newMessage);

        List<WebElement> content = controlPage.getVisibleAnnouncementsContent();
        String announcement = content.get(4).getText();
        assertTrue("Unexpected message content", announcement.equals(newMessage));
    }

    @Test
    public void testDeleteAnnouncement() {
        controlPage.goTo();

        List<WebElement> content = controlPage.getVisibleAnnouncementsContent();

        String announcement = content.get(2).getText();

        controlPage.clickDetailsButton(2);
        assertTrue("Announcement content doesn't match expected", announcement.equals(detailPage.getInputText()));

        detailPage.clickDeleteButton();

        assertEquals("Unexpected number of announcements", content.size() - 1, controlPage.getVisibleAnnouncementsContent().size());
    }

    @Test
    public void testDetailsTablePopulated() {
        controlPage.goTo();
        controlPage.clickDetailsButton(0);
        assertEquals("Unexpected number of user information rows in table", 6, detailPage.getTableDataSize());
    }

    @Test
    public void testMarkAnnouncementAsRead() {
        List<WebElement> announcements = dashboardPage.getCurrentUnreadAnnouncements();
        assertEquals("Unexpected number of announcements", 5, announcements.size());

        dashboardPage.markTopAnnouncementAsRead();

        announcements = dashboardPage.getCurrentUnreadAnnouncements();
        assertEquals("Unexpected number of announcements", 4, announcements.size());

        dashboardPage.viewReadAnnouncements();

        List<WebElement> readAnnouncements = readPage.getAllReadAnnouncements();
        assertEquals("Unexpected number of announcements displayed as read", 2, readAnnouncements.size());
    }

    /**
     * Checks if a List of {@link WebElement} is sorted in ascending order.
     *
     * @param elements
     * 		List of {@link WebElement}
     *
     * @return if the list is sorted ascending
     */
    private boolean checkSortedAscending(List<WebElement> elements) {
        boolean isSorted = true;
        for (int i = 1; i < elements.size(); i++) {
            if (elements.get(i).getText().compareTo(elements.get(i - 1).getText()) < 0) {
                isSorted = false;
                break;
            }
        }
        return isSorted;
    }

    /**
     * Checks if a list of {@link WebElement} is sorted in descending order.
     *
     * @param elements
     * 		List of {@link WebElement}
     *
     * @return if the list is sorted ascending
     */
    private boolean checkSortedDescending(List<WebElement> elements) {
        boolean isSorted = true;
        for (int i = 1; i < elements.size(); i++) {
            if (elements.get(i).getText().compareTo(elements.get(i - 1).getText()) > 0) {
                isSorted = false;
                break;
            }
        }
        return isSorted;
    }
}
