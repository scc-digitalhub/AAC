import * as React from 'react';
import { useState } from 'react';
import {
    useResourceContext,
    useGetResourceLabel,
    Button,
    useTranslate,
    Edit,
    EditButton,
    useEditContext,
    useDataProvider,
    useGetIdentity,
    SimpleList,
    FunctionToElement,
    RaRecord,
    DateField,
    Labeled,
    TextField,
    Toolbar,
    DeleteWithConfirmButton,
    LinearProgress,
    RecordContextProvider,
} from 'react-admin';
import { List, SimpleForm, TextInput } from 'react-admin';
import {
    Box,
    Grid,
    Typography,
    Card,
    ListItem,
    TableContainer,
    Paper,
    Table,
    TableBody,
    TableRow,
    TableCell,
    Avatar,
    Divider,
} from '@mui/material';
import Dialog from '@mui/material/Dialog';
import PersonIcon from '@mui/icons-material/Person';
import KeyIcon from '@mui/icons-material/Key';
import SwitchAccountIcon from '@mui/icons-material/SwitchAccount';
import EmailIcon from '@mui/icons-material/Email';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import PermContactCalendarIcon from '@mui/icons-material/PermContactCalendar';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

import { List as MuiList } from '@mui/material';
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import GridList from '../components/gridList';
import { PageTitle } from '../components/pageTitle';

const getIcon = (record: any) => {
    if (record.expireDate && record.expireDate < Date.now()) {
        return (
            <WarningAmberIcon
                fontSize="large"
                sx={{ color: 'text.disabled' }}
            />
        );
    }

    return <AppShortcutIcon fontSize="large" />;
};

export const ConnectionsList = () => {
    const translate = useTranslate();
    return (
        <Box component="div">
            <PageTitle
                text={translate('connections_page.header')}
                secondaryText={translate('connections_page.description')}
                icon={
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
                        <AppShortcutIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />
            <List component="div" pagination={false} actions={false}>
                <GridList
                    cols={6}
                    primaryText={record => {
                        return record.appName;
                    }}
                    icon={record => getIcon(record)}
                    tertiaryText={record => record.clientId}
                    secondaryText={record => (
                        <Box>
                            {record.appDescription && (
                                <Typography variant="subtitle1" sx={{ mb: 2 }}>
                                    {record.appDescription}
                                </Typography>
                            )}

                            <MuiList>
                                <ListItem sx={{ pl: 0 }}>
                                    <Labeled>
                                        <DateField
                                            label="created"
                                            source="createDate"
                                            showTime
                                        />
                                    </Labeled>
                                </ListItem>
                                <ListItem sx={{ pl: 0 }}>
                                    <Labeled>
                                        <DateField
                                            label="expires"
                                            source="expireDate"
                                            showTime
                                        />
                                    </Labeled>
                                </ListItem>
                            </MuiList>

                            {record.scopes && (
                                <Box>
                                    <Accordion elevation={0}>
                                        <AccordionSummary
                                            sx={{ pl: 0, m: 0 }}
                                            expandIcon={<ExpandMoreIcon />}
                                        >
                                            <Typography variant="subtitle1">
                                                {translate(
                                                    'connections_page.permissions',
                                                    {
                                                        num: record.scopes
                                                            .length,
                                                    }
                                                )}
                                            </Typography>
                                        </AccordionSummary>
                                        <AccordionDetails>
                                            <MuiList>
                                                {record.scopes.map(
                                                    (scope: RaRecord) => {
                                                        scope.id = scope.scope;
                                                        return (
                                                            <ListItem
                                                                sx={{ pl: 0 }}
                                                            >
                                                                <Box>
                                                                    <Typography color="textSecondary">
                                                                        <TextField
                                                                            source="scope"
                                                                            resource="scope"
                                                                            record={
                                                                                scope
                                                                            }
                                                                        />
                                                                    </Typography>
                                                                    <Typography
                                                                        variant="subtitle1"
                                                                        sx={{
                                                                            fontWeight: 600,
                                                                        }}
                                                                    >
                                                                        {
                                                                            scope.name
                                                                        }
                                                                    </Typography>
                                                                    <TextField
                                                                        label="description"
                                                                        source="description"
                                                                        resource="scope"
                                                                        record={
                                                                            scope
                                                                        }
                                                                    />
                                                                    <Divider variant="fullWidth" />
                                                                </Box>
                                                            </ListItem>
                                                        );
                                                    }
                                                )}
                                            </MuiList>
                                        </AccordionDetails>
                                    </Accordion>
                                </Box>
                            )}
                        </Box>
                    )}
                    actions={record => {
                        return (
                            <Toolbar variant="dense" sx={{ width: 1 }}>
                                <DeleteWithConfirmButton
                                    translateOptions={{ id: record.appName }}
                                />
                            </Toolbar>
                        );
                    }}
                />
            </List>
        </Box>
    );
};
