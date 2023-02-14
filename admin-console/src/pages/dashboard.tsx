import * as React from 'react';
import { Link } from 'react-router-dom';
import {
    useGetIdentity,
    useTranslate,
    useDataProvider,
    useGetOne,
} from 'react-admin';
import LinearProgress from '@mui/material/LinearProgress';
import {
    Card,
    CardContent,
    CardActions,
    CardHeader,
    Box,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
} from '@mui/material';
import { Container, Grid, Button, Avatar } from '@mui/material';

import SettingsIcon from '@mui/icons-material/Settings';
import WorkspacesIcon from '@mui/icons-material/Workspaces';
import KeyIcon from '@mui/icons-material/Key';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';
import DisplaySettingsIcon from '@mui/icons-material/DisplaySettings';

import { PageTitle } from '../components/pageTitle';
import { useEffect, useState } from 'react';

const AppPropsCard = () => {
    const translate = useTranslate();
    const dataProvider = useDataProvider();
    const [props, setProps] = useState();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState();

    useEffect(() => {
        dataProvider
            .appProps()
            .then(function (data: any) {
                setProps(data);
                setLoading(false);
            })
            .catch(function (error: any) {
                setError(error);
                setLoading(false);
            });
    }, []);

    return (
        <Card sx={{ height: '100%' }}>
            <CardHeader
                title={translate('page.dashboard.appProps.title')}
                avatar={<DisplaySettingsIcon />}
                titleTypographyProps={{ variant: 'h6' }}
            />
            <CardContent>
                {translate('page.dashboard.appProps.description')}
                {props && (
                    <Box>
                        <TableContainer sx={{ width: 1 }}>
                            <Table>
                                <TableBody>
                                    {Object.entries(props).map(
                                        (attr: any, key: any) => {
                                            return (
                                                <TableRow key={attr[0]}>
                                                    <TableCell
                                                        component="th"
                                                        scope="row"
                                                        sx={{
                                                            fontSize: '0.9rem',
                                                        }}
                                                    >
                                                        <strong>
                                                            {translate(
                                                                'field.' +
                                                                    attr[0]
                                                            )}
                                                        </strong>
                                                    </TableCell>
                                                    <TableCell align="right">
                                                        <span> {attr[1]} </span>
                                                    </TableCell>
                                                </TableRow>
                                            );
                                        }
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
};

const AdminDashboard = () => {
    const { data, isLoading } = useGetIdentity();
    const translate = useTranslate();
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
                                lineHeight: '102px',
                                backgroundColor: '#0066cc',
                            }}
                        >
                            <SettingsIcon sx={{ fontSize: 48 }} />
                        </Avatar>
                    )
                }
            />

            <Grid container spacing={2}>
                <Grid item xs={12} md={6} zeroMinWidth>
                    <AppPropsCard />
                </Grid>

                <Grid item xs={12} md={6} zeroMinWidth>
                    <Card sx={{ height: '100%' }}>
                        <CardHeader
                            title={translate('page.dashboard.realms.title')}
                            avatar={<WorkspacesIcon />}
                            titleTypographyProps={{ variant: 'h6' }}
                        />
                        <CardContent>
                            {translate('page.dashboard.realms.description')}
                        </CardContent>
                        <CardActions>
                            <Button component={Link} to="/realms">
                                {translate('page.dashboard.realms.manage')}
                            </Button>
                        </CardActions>
                    </Card>
                </Grid>
            </Grid>
        </Container>
    );
};

export default AdminDashboard;
