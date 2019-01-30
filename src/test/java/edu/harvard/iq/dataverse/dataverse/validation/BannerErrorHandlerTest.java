package edu.harvard.iq.dataverse.dataverse.validation;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.dataverse.banners.BannerLimits;
import edu.harvard.iq.dataverse.dataverse.banners.DataverseBanner;
import edu.harvard.iq.dataverse.dataverse.banners.DataverseLocalizedBanner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.omnifaces.util.Faces;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static edu.harvard.iq.dataverse.util.DateUtil.convertToDate;

@RunWith(MockitoJUnitRunner.class)
public class BannerErrorHandlerTest {

    @Mock
    private FacesContext facesContextMock;
    @Mock
    private ExternalContext externalContextMock;
    @Mock
    private Locale locale;
    @Captor
    private ArgumentCaptor<FacesMessage> facesMesssage;

    private static final Path BANNER_PATH = Paths.get("src/test/resources/images/banner.png");
    private static final java.util.Date FROM_TIME = convertToDate(
            LocalDateTime.of(2018, 12, 1, 9, 15, 45));
    private static final java.util.Date TO_TIME = convertToDate(
            LocalDateTime.of(2018, 11, 2, 10, 25, 55));

    @Before
    public void setup() {
        Faces.setContext(facesContextMock);

        Mockito.when(facesContextMock.getExternalContext()).thenReturn(externalContextMock);
        Mockito.when(externalContextMock.getRequestLocale()).thenReturn(locale);
        Mockito.when(externalContextMock.getRequestLocale().getLanguage()).thenReturn("en");
    }

    @Test
    public void shouldAddErrorMessageImageMissing() {
        //given
        BannerErrorHandler bannerErrorHandler = new BannerErrorHandler(new BannerLimits());
        DataverseBanner banner = new DataverseBanner();
        DataverseLocalizedBanner dataverseLocalizedBanner = new DataverseLocalizedBanner();
        dataverseLocalizedBanner.setImage(new byte[0]);
        banner.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBanner));

        //when
        bannerErrorHandler.handleBannerAddingErrors(banner, dataverseLocalizedBanner, facesContextMock);
        //then
        Mockito.verify(facesContextMock).addMessage(Mockito.eq("edit-text-messages-form:repeater:" + 0 + ":upload"),
                facesMesssage.capture());
        FacesMessage message = facesMesssage.getValue();

        Assert.assertEquals("The image is missing", message.getDetail());

    }

    @Test
    public void shouldAddErrorMessageResolutionTooHigh() throws IOException {
        //given
        BannerErrorHandler bannerErrorHandler = new BannerErrorHandler(new BannerLimits(1, 1, Integer.MAX_VALUE));
        DataverseBanner banner = new DataverseBanner();
        DataverseLocalizedBanner dataverseLocalizedBanner = new DataverseLocalizedBanner();
        dataverseLocalizedBanner.setImage(Files.readAllBytes(BANNER_PATH));
        banner.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBanner));

        //when
        bannerErrorHandler.handleBannerAddingErrors(banner, dataverseLocalizedBanner, facesContextMock);

        //then
        Mockito.verify(facesContextMock).addMessage(Mockito.eq("edit-text-messages-form:repeater:" + 0 + ":upload"),
                facesMesssage.capture());
        FacesMessage message = facesMesssage.getValue();

        Assert.assertEquals("The resolution was too big", message.getDetail());

    }

    @Test
    public void shouldAddErrorMessageSizeTooHigh() throws IOException {
        //given
        BannerErrorHandler bannerErrorHandler = new BannerErrorHandler(new BannerLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, 0));
        DataverseBanner banner = new DataverseBanner();
        DataverseLocalizedBanner dataverseLocalizedBanner = new DataverseLocalizedBanner();
        dataverseLocalizedBanner.setImage(Files.readAllBytes(BANNER_PATH));
        banner.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBanner));

        //when
        bannerErrorHandler.handleBannerAddingErrors(banner, dataverseLocalizedBanner, facesContextMock);

        //then
        Mockito.verify(facesContextMock).addMessage(Mockito.eq("edit-text-messages-form:repeater:" + 0 + ":second-file-warning"),
                facesMesssage.capture());
        FacesMessage message = facesMesssage.getValue();

        Assert.assertEquals("The image size was too big", message.getDetail());

    }

    @Test
    public void shouldValidateDate() throws IOException {
        //given
        BannerErrorHandler bannerErrorHandler = new BannerErrorHandler(new BannerLimits());
        DataverseBanner banner = new DataverseBanner();
        banner.setFromTime(FROM_TIME);
        banner.setToTime(TO_TIME);
        DataverseLocalizedBanner dataverseLocalizedBanner = new DataverseLocalizedBanner();
        banner.setDataverseLocalizedBanner(Lists.newArrayList(dataverseLocalizedBanner));
        dataverseLocalizedBanner.setImage(Files.readAllBytes(BANNER_PATH));

        //when
        bannerErrorHandler.handleBannerAddingErrors(banner, dataverseLocalizedBanner, facesContextMock);

        //then
        Mockito.verify(facesContextMock).addMessage(Mockito.eq("edit-text-messages-form:message-fromtime"),
                facesMesssage.capture());

        Mockito.verify(facesContextMock).addMessage(Mockito.eq("edit-text-messages-form:message-totime"),
                facesMesssage.capture());
        List<FacesMessage> allMessages = facesMesssage.getAllValues();

        Assert.assertEquals("End date must not be earlier than starting date.", allMessages.get(0).getDetail());
        Assert.assertEquals("End date must be a future date.", allMessages.get(1).getDetail());

    }
}
