import * as React from 'react';
import { Link } from 'react-router-dom';
import { useGetIdentity, useTranslate } from 'react-admin';
import LinearProgress from '@mui/material/LinearProgress';
import { Card, CardContent, CardActions, CardHeader } from '@mui/material';
import { Container, Grid, Button, Avatar } from '@mui/material';
import { useLocation } from 'react-router-dom';
import AppsIcon from '@mui/icons-material/Apps';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { PageTitle } from '../components/pageTitle';

const UserDashboard = () => {
    const { data, isLoading } = useGetIdentity();
    let url = useLocation();
    const translate = useTranslate();
    if (isLoading === true || !data) {
        return <LinearProgress />;
    }

    const regDomain = new RegExp('(?<=(/r/|/domains/))([^/]+)');
    const realmId = regDomain.test(url?.pathname)
        ? url?.pathname?.match(regDomain)![0] !== 'create'
            ? url?.pathname?.match(regDomain)![0]
            : ''
        : '';

    return (
        <Container maxWidth="lg">
            <PageTitle
                text={translate('page.dashboard.welcome', {
                    name: data.fullName,
                })}
                secondaryText={translate('page.dashboard.description')}
                icon={
                    data.username && (
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
                            {data.username.substring(0, 2)}
                        </Avatar>
                    )
                }
            />

            <Grid container spacing={2}>
                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate('page.dashboard.apps.title')}
                            avatar={<AppsIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate('page.dashboard.apps.description')}
                        </CardContent>
                        <CardActions>
                            <Button component={Link} to={`/apps/r/${realmId}`}>
                                {translate('page.dashboard.apps.manage')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate('page.dashboard.services.title')}
                            avatar={<MiscellaneousServicesIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate('page.dashboard.services.description')}
                        </CardContent>
                        <CardActions>
                            <Button
                                component={Link}
                                to={`/services/r/${realmId}`}
                            >
                                {translate('page.dashboard.services.manage')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate(
                                'page.dashboard.authentications.title'
                            )}
                            avatar={<VpnKeyIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate(
                                'page.dashboard.authentications.description'
                            )}
                        </CardContent>
                        <CardActions>
                            <Button
                                component={Link}
                                to={`/idps/r/${realmId}`}
                                size="small"
                            >
                                {translate(
                                    'page.dashboard.authentications.manage'
                                )}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>
            </Grid>
        </Container>
    );
};

export default UserDashboard;