import * as React from 'react';
import { Link } from 'react-router-dom';
import { useGetIdentity, useTranslate } from 'react-admin';
import LinearProgress from '@mui/material/LinearProgress';
import { Card, CardContent, CardActions, CardHeader } from '@mui/material';
import { Container, Grid, Typography, Button, Avatar } from '@mui/material';

import GroupIcon from '@mui/icons-material/Group';
import KeyIcon from '@mui/icons-material/Key';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';
import { PageTitle } from '../components/pageTitle';
import { transform } from 'typescript';

const UserDashboard = () => {
    const { data, isLoading } = useGetIdentity();
    const translate = useTranslate();
    if (isLoading === true || !data) {
        return <LinearProgress />;
    }
    return (
        <Container maxWidth="lg">
            <PageTitle
                text={translate('dashboard.welcome', { name: data.fullName })}
                secondaryText={translate('dashboard.description')}
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
                            title={translate('accounts')}
                            avatar={<GroupIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {' '}
                            {translate('dashboard.review_manage')}
                        </CardContent>
                        <CardActions>
                            <Button component={Link} to="/accounts">
                                {translate('dashboard.manage_accounts')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate('dashboard.credentials')}
                            avatar={<KeyIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {' '}
                            {translate('dashboard.view_update')}
                        </CardContent>
                        <CardActions>
                            <Button component={Link} to="/credentials">
                                {' '}
                                {translate('dashboard.credentials')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate('dashboard.third_party')}
                            avatar={<AppShortcutIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate('dashboard.view_and')}
                        </CardContent>
                        <CardActions>
                            <Button
                                component={Link}
                                to="/connections"
                                size="small"
                            >
                                {' '}
                                {translate('dashboard.manage_connections')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>
            </Grid>
        </Container>
    );
};

export default UserDashboard;
