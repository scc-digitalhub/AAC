import {
    Button,
    CreateButton,
    useCreatePath,
    useDataProvider,
    useGetIdentity,
    useGetResourceLabel,
    useTranslate,
} from 'react-admin';
import LinearProgress from '@mui/material/LinearProgress';
import { alpha, Box, Typography, useTheme } from '@mui/material';
import { Grid, Avatar } from '@mui/material';
import { PageTitle } from '../components/PageTitle';
import { useRootSelector } from '@dslab/ra-root-selector';
import { useEffect, useState } from 'react';
import { Page } from '../components/Page';
import {
    DashboardCard,
    DashboardHorizontalCard,
    DashboardTextCard,
    DashboardVerticalCard,
} from '../components/cards';
import UserLoginIcon from '@mui/icons-material/LoginOutlined';
import UserRegistrationIcon from '@mui/icons-material/HowToRegOutlined';
import TokenIcon from '@mui/icons-material/Token';
// import ContentAddIcon from '@mui/icons-material/Add';
import ContentAddIcon from '@mui/icons-material/AddBoxOutlined';
import { Link } from 'react-router-dom';

const DevDashboard = () => {
    const { data: user, isLoading } = useGetIdentity();
    const { root: realmId } = useRootSelector();
    const translate = useTranslate();
    const dataProvider = useDataProvider();
    const theme = useTheme();
    const bgColor = alpha(theme.palette?.primary?.main, 0.08);
    const getResourceLabel = useGetResourceLabel();
    const createPath = useCreatePath();

    const [stats, setStats] = useState<any>(null);

    useEffect(() => {
        let loading = true;
        dataProvider
            .invoke({ path: 'realms/' + realmId + '/stats' })
            .then(data => {
                if (data) {
                    setStats(data);
                }
            });

        return () => {
            loading = false;
        };
    }, [dataProvider]);

    if (isLoading === true || !user || !stats || !dataProvider) {
        return <LinearProgress />;
    }

    return (
        <Page>
            <PageTitle
                text={stats.realm.name}
                secondaryText={translate('page.dashboard.description')}
                icon={
                    stats.realm.name && (
                        <Avatar
                            sx={{
                                width: 72,
                                height: 72,
                                mb: 2,
                                alignItems: 'center',
                                display: 'inline-block',
                                textTransform: 'uppercase',
                                fontSize: 36,
                                fontWeight: 'bold',
                                lineHeight: '64px',
                                backgroundColor: '#0066cc',
                                // textAlign: 'center',
                            }}
                        >
                            {stats.realm.name.substring(0, 2)}
                        </Avatar>
                    )
                }
            />

            <Grid container spacing={2} mb={2}>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardCard
                        variant="text"
                        elevation={0}
                        number={stats.loginCount}
                        resource="audit"
                        // title="logins"
                        title={false}
                        text="dashboard.logins_7_days"
                        to={false}
                        icon={
                            <UserLoginIcon
                                color="primary"
                                fontSize="large"
                                sx={{ fontSize: '64px' }}
                            />
                        }
                    />
                </Grid>

                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardCard
                        variant="text"
                        elevation={0}
                        number={stats.registrationCount}
                        resource="audit"
                        // title="registrations"
                        title={false}
                        text="dashboard.registrations_7_days"
                        to={false}
                        icon={
                            <UserRegistrationIcon
                                color="primary"
                                fontSize="large"
                            />
                        }
                    />
                </Grid>

                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardCard
                        variant="text"
                        elevation={0}
                        number={stats.tokenCount}
                        resource="audit"
                        // title="tokens"
                        title={false}
                        text="dashboard.tokens_7_days"
                        to={false}
                        icon={<TokenIcon color="primary" fontSize="large" />}
                    />
                </Grid>
            </Grid>

            <Grid container spacing={2}>
                <Grid item xs={12} md={3} zeroMinWidth>
                    {stats.apps && stats.apps > 0 ? (
                        <DashboardCard number={stats.apps} resource="apps" />
                    ) : (
                        <DashboardCard
                            resource="apps"
                            secondaryText={<Empty resource="apps" />}
                            to={false}
                        />
                    )}
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    {stats.users && stats.users > 0 ? (
                        <DashboardCard number={stats.users} resource="users" />
                    ) : (
                        <DashboardCard
                            resource="users"
                            secondaryText={<Empty resource="users" />}
                            to={false}
                        />
                    )}
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    {stats.providers && stats.providers > 0 ? (
                        <DashboardCard
                            number={stats.providers}
                            resource="idps"
                        />
                    ) : (
                        <DashboardCard
                            resource="idps"
                            secondaryText={<Empty resource="idps" />}
                            to={false}
                        />
                    )}
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    {stats.services && stats.services > 0 ? (
                        <DashboardCard
                            number={stats.services}
                            resource="services"
                        />
                    ) : (
                        <DashboardCard
                            resource="services"
                            secondaryText={<Empty resource="services" />}
                            to={false}
                        />
                    )}
                </Grid>
            </Grid>
        </Page>
    );
};

const Empty = (props: { resource: string }) => {
    const { resource } = props;
    const createPath = useCreatePath();
    const translate = useTranslate();

    const getResourceLabel = useGetResourceLabel();
    const resourceName = translate(`resources.${resource}.forcedCaseName`, {
        smart_count: 0,
        _: getResourceLabel(resource, 0),
    });

    const emptyMessage = translate('ra.page.empty', { name: resourceName });
    const inviteMessage = translate('ra.page.invite');

    const to = createPath({ resource: resource, type: 'list' });

    return (
        <>
            <Box mb={2}>
                <Typography variant="h4" paragraph>
                    {translate(`resources.${resource}.empty`, {
                        _: emptyMessage,
                    })}
                </Typography>

                <Typography variant="body1">
                    {translate(`resources.${resource}.invite`, {
                        _: inviteMessage,
                    })}
                </Typography>
            </Box>
            <Button
                component={Link}
                to={to}
                label="ra.action.create"
                startIcon={<ContentAddIcon />}
                variant="text"
            />
        </>
    );
};

export default DevDashboard;
