import * as React from 'react';
import { Link } from 'react-router-dom';
import {
    useGetResourceLabel,
    useResourceDefinition,
    useTranslate,
    Button,
    useCreatePath,
} from 'react-admin';
import {
    Card,
    CardContent,
    CardHeader,
    alpha,
    useTheme,
    Stack,
    Divider,
    Typography,
    Box,
    Avatar,
} from '@mui/material';
import { Grid } from '@mui/material';
import { isValidElement, ReactElement } from 'react';
import LinkIcon from '@mui/icons-material/OpenInNew';
import { grey } from '@mui/material/colors';

export const DashboardCard = (props: {
    title?: string | false;
    resource?: string;
    number?: number;
    text?: string;
    icon?: ReactElement;
    secondaryText?: string | ReactElement;
    to?: string | false;
    variant?: 'text' | 'horizontal' | 'vertical';
    elevation?: number;
}) => {
    const {
        title,
        resource,
        number,
        icon,
        variant = 'text',
        elevation = 1,
        text = null,
        to = null,
        secondaryText,
    } = props;
    const translate = useTranslate();
    const theme = useTheme();
    const getResourceLabel = useGetResourceLabel();
    const definition = useResourceDefinition({ resource });
    const createPath = useCreatePath();

    const resourceLabel = resource ? getResourceLabel(resource, 2) : '';
    const displayTitle = title ? title : resourceLabel ? resourceLabel : '';
    const displayText = text
        ? translate(text)
        : resource
        ? getResourceLabel(resource, number ?? 2)
        : resourceLabel;

    const iconProps = {
        fontSize: 'large',
        sx: {
            fontSize: variant == 'horizontal' ? '96px' : '32px',
            height: variant == 'horizontal' ? '96px' : '32px',
            width: variant == 'horizontal' ? '96px' : '32px',
            color: theme.palette.primary.light,
        },
    };
    const displayIcon =
        icon && React.isValidElement(icon) ? (
            icon
        ) : resource && definition?.icon ? (
            React.createElement(definition.icon, iconProps)
        ) : (
            <Avatar
                sx={{
                    mt: 1,
                    backgroundColor: grey[200],
                    marginX: 'auto',
                    lineHeight: '50%',
                    ...iconProps.sx,
                }}
            >
                {displayTitle.substring(0, 2)}
            </Avatar>
        );

    const linkTo = to
        ? to
        : resource
        ? createPath({
              resource: resource,
              type: 'list',
          })
        : false;

    if (variant == 'horizontal') {
        return (
            <DashboardHorizontalCard
                title={title !== false ? displayTitle : false}
                icon={displayIcon}
                text={displayText}
                number={number}
                secondaryText={secondaryText}
                to={to === false ? false : linkTo}
                elevation={elevation}
            />
        );
    }
    if (variant == 'vertical') {
        return (
            <DashboardVerticalCard
                title={title !== false ? displayTitle : false}
                icon={displayIcon}
                text={displayText}
                number={number}
                secondaryText={secondaryText}
                to={to === false ? false : linkTo}
                elevation={elevation}
            />
        );
    }
    if (variant == 'text') {
        return (
            <DashboardTextCard
                title={title !== false ? displayTitle : false}
                icon={displayIcon}
                text={displayText}
                number={number}
                secondaryText={secondaryText}
                to={to === false ? false : linkTo}
                elevation={elevation}
            />
        );
    }
    return null;
};

export const DashboardVerticalCard = (props: {
    title: string | false;
    icon: ReactElement;
    text?: string;
    number?: number;
    secondaryText?: string | ReactElement;
    to?: string | false;
    elevation?: number;
}) => {
    const {
        title,
        icon,
        number,
        text,
        secondaryText,
        to,
        elevation = 1,
    } = props;
    const translate = useTranslate();
    const theme = useTheme();

    return (
        <Card
            elevation={elevation}
            sx={{
                height: '100%',
                position: 'relative',
                px: 1,
                borderBottom:
                    elevation && elevation > 0
                        ? '2px solid ' + theme.palette.primary.main
                        : '0 none',
            }}
        >
            <CardHeader
                title={icon}
                subheader={title !== false ? translate(title) : undefined}
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
                {secondaryText ? <p>{secondaryText}</p> : ''}
                <Divider sx={{ borderColor: grey[200], mb: 2 }} />
                <Grid container spacing={1}>
                    <Grid
                        item
                        xs={to ? 6 : 12}
                        sx={{ textAlign: 'center', pt: 1 }}
                        alignItems="center"
                    >
                        {number !== undefined && (
                            <Typography color={grey[800]} variant="h3">
                                {number}
                            </Typography>
                        )}
                        {text && (
                            <Typography
                                component={'span'}
                                // color={}
                                variant="body2"
                            >
                                {translate(text)}
                            </Typography>
                        )}
                    </Grid>
                    {to && (
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
                            <Button component={Link} to={to}>
                                <Stack
                                    direction={'column'}
                                    alignItems={'center'}
                                >
                                    <LinkIcon fontSize="large" />
                                    <Typography
                                        component={'span'}
                                        variant="body2"
                                        textTransform={'uppercase'}
                                        textAlign={'center'}
                                    >
                                        {translate('action.manage')}
                                    </Typography>
                                </Stack>
                            </Button>
                        </Grid>
                    )}
                </Grid>
            </CardContent>
        </Card>
    );
};

export const DashboardHorizontalCard = (props: {
    title: string | false;
    icon: ReactElement;
    text?: string;
    secondaryText?: string | ReactElement;
    number?: number;
    to?: string | false;
    elevation?: number;
}) => {
    const { title, icon, number, text, secondaryText, to, elevation } = props;
    const translate = useTranslate();
    const theme = useTheme();

    const bgColor = alpha(theme.palette?.primary?.main, 0.08);

    return (
        <Card
            elevation={elevation}
            sx={{
                height: '100%',
                position: 'relative',
                px: 1,
                borderBottom:
                    elevation && elevation > 0
                        ? '2px solid ' + theme.palette.primary.main
                        : '0 none',
            }}
        >
            <CardContent>
                <Grid container spacing={1} alignItems="center">
                    <Grid
                        item
                        xs={6}
                        alignItems="center"
                        sx={{ textAlign: 'center' }}
                    >
                        {icon}
                    </Grid>
                    <Grid
                        item
                        xs={6}
                        sx={{ textAlign: 'center', pt: 1 }}
                        alignItems="center"
                    >
                        {title && (
                            <CardHeader
                                title={translate(title)}
                                titleTypographyProps={{
                                    variant: 'h6',
                                    textTransform: 'uppercase',
                                    color: grey[700],
                                }}
                            />
                        )}
                        {number !== undefined && (
                            <Box>
                                <Typography color={grey[800]} variant="h3">
                                    {number}
                                </Typography>
                                <Typography
                                    component={'span'}
                                    // color={}
                                    variant="body2"
                                >
                                    {text}
                                </Typography>
                            </Box>
                        )}
                        <Divider sx={{ my: 2, borderColor: bgColor }} />

                        {to && (
                            <Button
                                component={Link}
                                to={to}
                                label="action.manage"
                            >
                                <LinkIcon fontSize="large" />
                            </Button>
                        )}
                    </Grid>
                </Grid>
                {secondaryText &&
                    (isValidElement(secondaryText) ? (
                        secondaryText
                    ) : (
                        <Typography component={'span'} variant="body2">
                            {secondaryText}
                        </Typography>
                    ))}
            </CardContent>
        </Card>
    );
};

export const DashboardTextCard = (props: {
    title: string | false;
    icon: ReactElement;
    resource?: string;
    text?: string;
    number?: number;
    secondaryText?: string | ReactElement;
    to?: string | false;
    elevation?: number;
}) => {
    const { title, icon, number, text, secondaryText, to, elevation } = props;
    const translate = useTranslate();
    const theme = useTheme();
    const bgColor = alpha(theme.palette?.primary?.main, 0.08);

    return (
        <Card
            elevation={elevation}
            sx={{
                height: '100%',
                position: 'relative',
                px: 1,
                borderBottom:
                    elevation && elevation > 0
                        ? '2px solid ' + theme.palette.primary.main
                        : '0 none',
            }}
        >
            {title && (
                <CardHeader
                    title={translate(title)}
                    titleTypographyProps={{
                        variant: 'body2',
                        textTransform: 'uppercase',
                        color: theme.palette.primary.main,
                    }}
                    avatar={icon}
                />
            )}
            <CardContent sx={{ px: 3 }}>
                {number !== undefined && (
                    <Box>
                        <Typography color={grey[800]} variant="h2">
                            {number}
                        </Typography>
                        {text && (
                            <Typography component={'span'} variant="body2">
                                {translate(text)}
                            </Typography>
                        )}
                    </Box>
                )}
                {secondaryText &&
                    (isValidElement(secondaryText) ? (
                        secondaryText
                    ) : (
                        <Typography component={'span'} variant="body2">
                            {secondaryText}
                        </Typography>
                    ))}
                {to && (
                    <>
                        <Divider sx={{ my: 2, borderColor: bgColor }} />

                        <Button component={Link} to={to} label="action.manage">
                            <LinkIcon fontSize="large" />
                        </Button>
                    </>
                )}
            </CardContent>
        </Card>
    );
};
