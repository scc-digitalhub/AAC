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
    useGetResourceLabel,
    useResourceDefinition,
    useTranslate,
    Button,
    useCreatePath,
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
    Divider,
    Typography,
    Box,
} from '@mui/material';
import { Container, Grid, Avatar } from '@mui/material';
import { useLocation } from 'react-router-dom';
import AppsIcon from '@mui/icons-material/Apps';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { PageTitle } from '../components/PageTitle';
import { useRootSelector } from '@dslab/ra-root-selector';
import { ReactElement, useEffect, useState } from 'react';
import { CounterBadge } from '../components/CounterBadge';
import { Page } from '../components/Page';
import LinkIcon from '@mui/icons-material/OpenInNew';
import { grey } from '@mui/material/colors';

const DevDashboard = () => {
    const { data: user, isLoading } = useGetIdentity();
    const { root: realmId } = useRootSelector();
    const translate = useTranslate();
    const [apps, setApps] = useState<number>();
    const [services, setServices] = useState<number>();
    const [idps, setIdps] = useState<number>();
    const [users, setUsers] = useState<number>();
    const dataProvider = useDataProvider();
    const { data: realm, error } = useGetOne('myrealms', { id: realmId });
    const theme = useTheme();
    const bgColor = alpha(theme.palette?.primary?.main, 0.08);

    useEffect(() => {
        let loading = true;
        const params = {
            pagination: { page: 1, perPage: 1 },
            sort: { field: 'name', order: 'ASC' } as SortPayload,
            filter: {},
        };

        dataProvider.getList('apps', params).then(res => {
            if (res.data && loading) {
                setApps(res.total);
            }
        });
        dataProvider.getList('services', params).then(res => {
            if (res.data && loading) {
                setServices(res.total);
            }
        });
        dataProvider.getList('idps', params).then(res => {
            if (res.data && loading) {
                setIdps(res.total);
            }
        });
        dataProvider
            .getList('users', {
                ...params,
                sort: { field: 'username', order: 'ASC' },
            })
            .then(res => {
                if (res.data && loading) {
                    setUsers(res.total);
                }
            });

        return () => {
            loading = false;
        };
    }, [dataProvider]);

    if (isLoading === true || !user || !realm || !dataProvider) {
        return <LinearProgress />;
    }
    if (error) {
        return <p>ERROR</p>;
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
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardHorizontalCard number={apps} resource="apps" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardHorizontalCard number={users} resource="users" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardHorizontalCard number={idps} resource="idps" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardHorizontalCard
                        number={services}
                        resource="services"
                    />
                </Grid>
            </Grid>
            <br />
            <Grid container spacing={2}>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardVerticalCard number={apps} resource="apps" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardVerticalCard number={users} resource="users" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardVerticalCard number={idps} resource="idps" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardVerticalCard
                        number={services}
                        resource="services"
                    />
                </Grid>
            </Grid>
            <br />
            <Grid container spacing={2}>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardTextCard number={apps} resource="apps" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardTextCard number={users} resource="users" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardTextCard number={idps} resource="idps" />
                </Grid>
                <Grid item xs={12} md={3} zeroMinWidth>
                    <DashboardTextCard number={services} resource="services" />
                </Grid>
            </Grid>
        </Page>
    );
};

export const DashboardVerticalCard = (props: {
    title?: string;
    resource?: string;
    text?: string;
    icon?: ReactElement;
    number?: number;
    to?: string;
}) => {
    const { resource, number, icon, title, text = null, to = null } = props;
    const translate = useTranslate();
    const theme = useTheme();
    const getResourceLabel = useGetResourceLabel();
    const definition = useResourceDefinition({ resource });
    const createPath = useCreatePath();

    const displayTitle = title
        ? title
        : resource
        ? getResourceLabel(resource, 2)
        : '';

    const iconProps = {
        color: 'primary',
        fontSize: 'large',
        sx: { fontSize: '96px' },
    };

    const displayIcon =
        icon && React.isValidElement(icon)
            ? icon
            : resource && definition?.icon
            ? React.createElement(definition.icon, iconProps)
            : undefined;

    const bgColor = alpha(theme.palette?.primary?.main, 0.08);
    const linkTo = to
        ? to
        : resource
        ? createPath({
              resource: resource,
              type: 'list',
          })
        : null;

    return (
        <Card
            sx={{
                height: '100%',
                position: 'relative',
                px: 1,
                borderBottom: '2px solid ' + theme.palette.primary.main,
                // borderBottomColor: theme.palette.primary,
            }}
        >
            <CardHeader
                // title={translate(displayTitle)}
                // avatar={displayIcon}
                title={displayIcon}
                subheader={translate(displayTitle)}
                titleTypographyProps={{
                    variant: 'h5',
                    textAlign: 'center',
                }}
                subheaderTypographyProps={{
                    variant: 'h6',
                    textAlign: 'center',
                    color: grey[900],
                    textTransform: 'uppercase',
                }}
            />

            <CardContent>
                {text ? <p>translate(text)</p> : ''}
                <Divider sx={{ borderColor: grey[200], mb: 2 }} />
                <Grid container spacing={1}>
                    <Grid
                        item
                        xs={6}
                        sx={{ textAlign: 'center', pt: 1 }}
                        alignItems="center"
                    >
                        {number && (
                            <Typography color={grey[800]} variant="h3">
                                {number}
                            </Typography>
                        )}
                    </Grid>
                    <Grid
                        item
                        xs={6}
                        sx={{
                            textAlign: 'center',
                            pt: 1,
                            borderLeft: '1px solid',
                            borderLeftColor: grey[200],
                        }}
                        alignItems="center"
                    >
                        {linkTo && (
                            <Button
                                component={Link}
                                to={linkTo}
                                // label="ra.action.list"
                            >
                                <Stack
                                    direction={'column'}
                                    alignItems={'center'}
                                >
                                    <LinkIcon fontSize="large" />
                                    <Typography
                                        component={'span'}
                                        // color={}
                                        variant="body2"
                                        textTransform={'uppercase'}
                                        textAlign={'center'}
                                    >
                                        {translate('action.manage')}
                                    </Typography>
                                </Stack>
                            </Button>
                        )}
                    </Grid>
                </Grid>
            </CardContent>
        </Card>
    );
};

export const DashboardHorizontalCard = (props: {
    title?: string;
    resource?: string;
    text?: string;
    icon?: ReactElement;
    number?: number;
    to?: string;
}) => {
    const { resource, number, icon, title, text = null, to = null } = props;
    const translate = useTranslate();
    const theme = useTheme();
    const getResourceLabel = useGetResourceLabel();
    const definition = useResourceDefinition({ resource });
    const createPath = useCreatePath();

    const resourceLabel = resource ? getResourceLabel(resource, 2) : null;
    const displayTitle = title ? title : '';
    const displayText = text
        ? translate(text)
        : resource
        ? getResourceLabel(resource, number ?? 2)
        : resourceLabel;
    const bgColor = alpha(theme.palette?.primary?.main, 0.08);

    const iconProps = {
        fontSize: 'large',
        sx: {
            fontSize: '128px',
            color: theme.palette.primary.light,
        },
    };
    const displayIcon =
        icon && React.isValidElement(icon)
            ? icon
            : resource && definition?.icon
            ? React.createElement(definition.icon, iconProps)
            : undefined;

    const linkTo = to
        ? to
        : resource
        ? createPath({
              resource: resource,
              type: 'list',
          })
        : null;

    return (
        <Card
            sx={{
                height: '100%',
                position: 'relative',
                px: 1,
                borderBottom: '2px solid ' + theme.palette.primary.main,
                // borderBottomColor: theme.palette.primary,
            }}
        >
            <CardContent>
                <Grid container spacing={1} alignItems="center">
                    <Grid
                        item
                        xs={6}
                        // sx={{ textAlign: 'center }}
                        alignItems="center"
                    >
                        {displayIcon}
                    </Grid>
                    <Grid
                        item
                        xs={6}
                        sx={{ textAlign: 'center', pt: 1 }}
                        alignItems="center"
                    >
                        <CardHeader
                            title={translate(displayTitle)}
                            titleTypographyProps={{
                                variant: 'h6',
                                textTransform: 'uppercase',
                                color: grey[700],
                            }}
                        />
                        {number && (
                            <Box>
                                <Typography color={grey[800]} variant="h3">
                                    {number}
                                </Typography>
                                <Typography
                                    component={'span'}
                                    // color={}
                                    variant="body2"
                                >
                                    {displayText}
                                </Typography>
                            </Box>
                        )}
                        <Divider sx={{ my: 2, borderColor: bgColor }} />
                        {linkTo && (
                            <Button
                                component={Link}
                                to={linkTo}
                                label="action.manage"
                            >
                                <LinkIcon fontSize="large" />
                            </Button>
                        )}
                    </Grid>
                </Grid>
            </CardContent>
        </Card>
    );
};

export const DashboardTextCard = (props: {
    title?: string;
    resource?: string;
    text?: string;
    icon?: ReactElement;
    number?: number;
    to?: string;
}) => {
    const { resource, number, icon, title, text = null, to = null } = props;
    const translate = useTranslate();
    const theme = useTheme();
    const getResourceLabel = useGetResourceLabel();
    const definition = useResourceDefinition({ resource });
    const createPath = useCreatePath();

    const resourceLabel = resource ? getResourceLabel(resource, 2) : null;
    const displayTitle = title ? title : resourceLabel ? resourceLabel : '';
    const displayText = text
        ? translate(text)
        : resource
        ? getResourceLabel(resource, number ?? 2)
        : resourceLabel;
    const bgColor = alpha(theme.palette?.primary?.main, 0.08);

    const iconProps = {
        fontSize: 'large',
        sx: {
            color: theme.palette.primary.light,
        },
    };
    const displayIcon =
        icon && React.isValidElement(icon)
            ? icon
            : resource && definition?.icon
            ? React.createElement(definition.icon, iconProps)
            : undefined;

    const linkTo = to
        ? to
        : resource
        ? createPath({
              resource: resource,
              type: 'list',
          })
        : null;

    return (
        <Card
            sx={{
                height: '100%',
                position: 'relative',
                px: 1,
                borderBottom: '2px solid ' + theme.palette.primary.main,
            }}
        >
            <CardHeader
                title={translate(displayTitle)}
                titleTypographyProps={{
                    variant: 'body2',
                    textTransform: 'uppercase',
                    color: theme.palette.primary.main,
                }}
                avatar={displayIcon}
            />
            <CardContent sx={{ px: 3 }}>
                {number && (
                    <Box>
                        <Typography color={grey[800]} variant="h2">
                            {number}
                        </Typography>
                        <Typography
                            component={'span'}
                            // color={}
                            variant="body2"
                        >
                            {displayText}
                        </Typography>
                    </Box>
                )}
                <Divider sx={{ my: 2, borderColor: bgColor }} />
                {linkTo && (
                    <Button component={Link} to={linkTo} label="action.manage">
                        <LinkIcon fontSize="large" />
                    </Button>
                )}
            </CardContent>
        </Card>
    );
};

export default DevDashboard;
