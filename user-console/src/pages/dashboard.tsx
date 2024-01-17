import * as React from 'react';
import { Link } from 'react-router-dom';
import { useGetIdentity, useGetList, useTranslate } from 'react-admin';
import LinearProgress from '@mui/material/LinearProgress';
import {
    Card,
    CardContent,
    CardActions,
    CardHeader,
    Alert,
} from '@mui/material';
import { Container, Grid, Button, Avatar } from '@mui/material';

import GroupIcon from '@mui/icons-material/Group';
import KeyIcon from '@mui/icons-material/Key';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';
import { PageTitle } from '../components/pageTitle';
import { useEffect, useState } from 'react';

const UserDashboard = () => {
    const translate = useTranslate();
    const { data, isLoading } = useGetIdentity();
    //load accounts and credential to check if we should suggest actions
    //TODO refactor
    const { data: accounts, isLoading: isLoadingAccounts } = useGetList(
        'accounts',
        {
            pagination: { page: 1, perPage: 100 },
        }
    );
    const { data: passwords, isLoading: isLoadingPasswords } = useGetList(
        'password',
        {
            pagination: { page: 1, perPage: 100 },
        }
    );
    const { data: keys, isLoading: isLoadingKeys } = useGetList('webauthn', {
        pagination: { page: 1, perPage: 100 },
    });

    const [shouldCreateCredentials, setShouldCreateCredentials] =
        useState<boolean>(false);

    useEffect(() => {
        if (!isLoadingAccounts && !isLoadingPasswords && !isLoadingKeys) {
            const account = accounts?.find(a => a.authority === 'internal');
            if (account && passwords?.length === 0 && keys?.length === 0) {
                setShouldCreateCredentials(true);
            }
        }
    }, [
        accounts,
        passwords,
        keys,
        isLoadingAccounts,
        isLoadingPasswords,
        isLoadingKeys,
    ]);

    if (isLoading === true || !data) {
        return <LinearProgress />;
    }

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
                            title={translate('page.dashboard.accounts.title')}
                            avatar={<GroupIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate('page.dashboard.accounts.description')}
                        </CardContent>
                        <CardActions>
                            <Button component={Link} to="/accounts">
                                {translate('page.dashboard.accounts.manage')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate(
                                'page.dashboard.credentials.title'
                            )}
                            avatar={<KeyIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate(
                                'page.dashboard.credentials.description'
                            )}
                            {shouldCreateCredentials && (
                                <Alert
                                    severity="warning"
                                    variant="filled"
                                    sx={{ width: '100%', mt: 2 }}
                                >
                                    {translate('alert.missing_credentials')}
                                </Alert>
                            )}
                        </CardContent>
                        <CardActions>
                            <Button component={Link} to="/credentials">
                                {translate('page.dashboard.credentials.manage')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate(
                                'page.dashboard.connections.title'
                            )}
                            avatar={<AppShortcutIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate(
                                'page.dashboard.connections.description'
                            )}
                        </CardContent>
                        <CardActions>
                            <Button
                                component={Link}
                                to="/connections"
                                size="small"
                            >
                                {translate('page.dashboard.connections.manage')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>
            </Grid>
        </Container>
    );
};

export default UserDashboard;
