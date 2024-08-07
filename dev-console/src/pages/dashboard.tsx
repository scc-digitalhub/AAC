import * as React from 'react';
import { Link } from 'react-router-dom';
import {
    ListButton,
    ListView,
    LoadingIndicator,
    SortPayload,
    useDataProvider,
    useGetIdentity,
    useGetOne,
    useTranslate,
} from 'react-admin';
import LinearProgress from '@mui/material/LinearProgress';
import {
    Card,
    CardContent,
    CardActions,
    CardHeader,
    alpha,
    useTheme,
    Stack,
} from '@mui/material';
import { Container, Grid, Button, Avatar } from '@mui/material';
import { useLocation } from 'react-router-dom';
import AppsIcon from '@mui/icons-material/Apps';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { PageTitle } from '../components/PageTitle';
import { useRootSelector } from '@dslab/ra-root-selector';
import { useEffect, useState } from 'react';
import { CounterBadge } from '../components/CounterBadge';
import { Page } from '../components/Page';

const DevDashboard = () => {
    const { data: user, isLoading } = useGetIdentity();
    const { root: realmId } = useRootSelector();
    const translate = useTranslate();
    const [apps, setApps] = useState<number>();
    const [services, setServices] = useState<number>();
    const [idps, setIdps] = useState<number>();
    const dataProvider = useDataProvider();
    const { data: realm, error } = useGetOne('myrealms', { id: realmId });
    const theme = useTheme();
    const bgColor = alpha(theme.palette?.primary?.main, 0.08);

    useEffect(() => {
        const params = {
            pagination: { page: 1, perPage: 5 },
            sort: { field: 'updated', order: 'DESC' } as SortPayload,
            filter: {},
        };
        dataProvider.getList('apps', params).then(res => {
            if (res.data) {
                setApps(res.total);
            }
        });
        dataProvider.getList('services', params).then(res => {
            if (res.data) {
                setServices(res.total);
            }
        });
        dataProvider.getList('idps', params).then(res => {
            if (res.data) {
                setIdps(res.total);
            }
        });
    }, [dataProvider]);

    if (isLoading === true || !user || !realm) {
        return <LinearProgress />;
    }
    if (error) {
        return <p>ERROR</p>;
    }

    if (!dataProvider) {
        return <LoadingIndicator />;
    }
    return (
        <Page>
            <PageTitle
                text={realm.name}
                secondaryText={translate('page.dashboard.description')}
                icon={
                    realm.name && (
                        <Avatar
                            sx={{
                                width: 72,
                                height: 72,
                                mb: 2,
                                alignItems: 'center',
                                display: 'inline-block',
                                textTransform: 'uppercase',
                                fontSize: 36,
                                lineHeight: '64px',
                                backgroundColor: '#0066cc',
                            }}
                        >
                            {realm.name.substring(0, 2)}
                        </Avatar>
                    )
                }
            />

            <Grid container spacing={2}>
                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%', position: 'relative' }}>
                        <CardHeader
                            title={translate('page.dashboard.apps.title')}
                            avatar={<AppsIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            <div>
                                {translate('page.dashboard.apps.description')}
                            </div>
                            {apps && apps > 0 && (
                                <Stack
                                    direction="row"
                                    alignItems="center"
                                    sx={{ mt: 2 }}
                                >
                                    <CounterBadge
                                        value={apps}
                                        color="secondary.main"
                                        backgroundColor={bgColor}
                                        size="large"
                                    />
                                    {translate('page.dashboard.number.apps')}
                                </Stack>
                            )}
                        </CardContent>
                        <CardActions
                            sx={{
                                position: !apps ? 'absolute' : '',
                                bottom: 0,
                            }}
                        >
                            <ListButton
                                resource="apps"
                                label={translate('page.dashboard.apps.manage')}
                            />
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%', position: 'relative' }}>
                        <CardHeader
                            title={translate('page.dashboard.services.title')}
                            avatar={<MiscellaneousServicesIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            <div>
                                {' '}
                                {translate(
                                    'page.dashboard.services.description'
                                )}
                            </div>
                            {services && services > 0 && (
                                <Stack
                                    direction="row"
                                    alignItems="center"
                                    sx={{ mt: 2 }}
                                >
                                    <CounterBadge
                                        value={services}
                                        color="secondary.main"
                                        backgroundColor={bgColor}
                                        size="large"
                                    />

                                    {translate(
                                        'page.dashboard.number.services'
                                    )}
                                </Stack>
                            )}
                        </CardContent>
                        <CardActions
                            sx={{
                                position: !services ? 'absolute' : '',
                                bottom: 0,
                            }}
                        >
                            <ListButton
                                resource="services"
                                label={translate(
                                    'page.dashboard.services.manage'
                                )}
                            />
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%', position: 'relative' }}>
                        <CardHeader
                            title={translate(
                                'page.dashboard.authentications.title'
                            )}
                            avatar={<VpnKeyIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            <div>
                                {translate(
                                    'page.dashboard.authentications.description'
                                )}
                            </div>
                            {idps && idps > 0 && (
                                <Stack
                                    direction="row"
                                    alignItems="center"
                                    sx={{ mt: 2 }}
                                >
                                    <CounterBadge
                                        value={idps}
                                        color="secondary.main"
                                        backgroundColor={bgColor}
                                        size="large"
                                    />
                                    {translate('page.dashboard.number.idp')}
                                </Stack>
                            )}
                        </CardContent>
                        <CardActions
                            sx={{
                                position: !idps ? 'absolute' : '',
                                bottom: 0,
                            }}
                        >
                            <ListButton
                                resource="idps"
                                label={translate(
                                    'page.dashboard.authentications.manage'
                                )}
                            />
                        </CardActions>
                    </Card>
                </Grid>
            </Grid>
        </Page>
    );
};

export default DevDashboard;
