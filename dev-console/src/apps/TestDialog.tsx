import {
    Box,
    Breakpoint,
    Dialog,
    DialogContent,
    DialogTitle,
    FormControl,
    FormLabel,
    Grid,
    IconButton,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    styled,
    Typography,
} from '@mui/material';
import { Fragment, ReactElement, useCallback, useState } from 'react';
import {
    RaRecord,
    ButtonProps,
    useRecordContext,
    useResourceContext,
    useTranslate,
    Button,
    LoadingIndicator,
    useDataProvider,
    useNotify,
    Labeled,
} from 'react-admin';
import { useRootSelector } from '@dslab/ra-root-selector';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-solarized_dark';

import CloseIcon from '@mui/icons-material/Close';
import { IdField } from '../components/IdField';
import TestIcon from '@mui/icons-material/DirectionsRun';
const defaultIcon = <TestIcon />;

export const TestDialogButton = (props: TestDialogButtonProps) => {
    const {
        label = 'action.test',
        icon = defaultIcon,
        fullWidth = true,
        maxWidth = 'lg',
        color = 'success',
        record: recordFromProps,
        resource: resourceFromProps,
        ...rest
    } = props;
    const translate = useTranslate();
    const [open, setOpen] = useState(false);

    const resourceContext = useResourceContext();
    const recordContext = useRecordContext();

    const resource = resourceFromProps || resourceContext;
    const record = recordFromProps || recordContext;
    const isLoading = !record;

    const handleDialogOpen = e => {
        setOpen(true);
        e.stopPropagation();
    };

    const handleDialogClose = e => {
        setOpen(false);
        e.stopPropagation();
    };
    const handleClick = useCallback(e => {
        e.stopPropagation();
    }, []);

    return (
        <Fragment>
            <Button
                label={label}
                onClick={handleDialogOpen}
                color={color}
                {...rest}
            >
                {icon}
            </Button>
            <TestDialog
                open={open}
                onClose={handleDialogClose}
                onClick={handleClick}
                fullWidth={fullWidth}
                maxWidth={maxWidth}
            >
                <div className={TestDialogButtonClasses.header}>
                    <DialogTitle
                        id="test-dialog-title"
                        className={TestDialogButtonClasses.title}
                    >
                        {translate(label)}
                    </DialogTitle>
                    <IconButton
                        aria-label={translate('ra.action.close')}
                        title={translate('ra.action.close')}
                        onClick={handleDialogClose}
                        size="small"
                        className={TestDialogButtonClasses.closeButton}
                    >
                        <CloseIcon fontSize="small" />
                    </IconButton>
                </div>

                {isLoading ? (
                    <LoadingIndicator />
                ) : (
                    <TestEditDialog
                        record={record}
                        resource={resource}
                        handleClose={handleDialogClose}
                    />
                )}
            </TestDialog>
        </Fragment>
    );
};

const TestEditDialog = (props: {
    record: any;
    resource: string;
    handleClose: (e: any) => void;
    onSuccess?: () => void;
}) => {
    const { record, resource, handleClose, onSuccess } = props;
    const translate = useTranslate();
    const dataProvider = useDataProvider();
    const notify = useNotify();
    const { root: realmId } = useRootSelector();
    const [flow, setFlow] = useState<string>();
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [token, setToken] = useState<any>({
        access_token: null,
        access_token_decoded: null,
    });

    if (!record || !resource) return null;
    if (isLoading) return <LoadingIndicator />;

    const flows = record.configuration?.authorizedGrantTypes || [];

    const handleChange = e => {
        setFlow(e.target.value);
    };

    const handleTest = e => {
        if (flow) {
            dataProvider
                .invoke({
                    path:
                        'apps/' + realmId + '/' + record.id + '/oauth2/' + flow,
                })
                .then(json => {
                    if (json) {
                        const t = {
                            ...json,
                            access_token_decoded: json.access_token?.includes(
                                '.'
                            )
                                ? JSON.stringify(
                                      JSON.parse(
                                          atob(json.access_token.split('.')[1])
                                      ),
                                      null,
                                      2
                                  )
                                : null,
                        };
                        setToken(t);
                    } else {
                        notify('ra.notification.bad_item', { type: 'warning' });
                    }
                })
                .catch(error => {
                    const msg = error.message || 'ra.notification.error';
                    notify(msg, { type: 'error' });
                });
        }
    };

    return (
        <DialogContent>
            <FormControl component="fieldset" fullWidth>
                <FormLabel component="legend" sx={{ mb: 2 }}>
                    {translate('dialog.test.helperText')}
                </FormLabel>
                <FormControl fullWidth>
                    <InputLabel>{translate('oauth2.flows.title')}</InputLabel>
                    <Select
                        value={flow}
                        label={translate('oauth2.flows.title')}
                        onChange={handleChange}
                    >
                        {flows.map(f => (
                            <MenuItem value={f} key={f}>
                                {translate('oauth2.flows.' + f)}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
                {flow && (
                    <Box mt={2}>
                        <Grid container mb={1}>
                            <Grid item xs={10}>
                                <Stack rowGap={1}>
                                    <Typography variant="h6">
                                        {translate(
                                            'oauth2.flows.' + flow + '.name'
                                        )}
                                    </Typography>
                                    <Typography
                                        variant="body2"
                                        fontFamily={'monospace'}
                                    >
                                        {flow}
                                    </Typography>
                                    <Typography variant="body2">
                                        {translate(
                                            'oauth2.flows.' +
                                                flow +
                                                '.description'
                                        )}
                                    </Typography>
                                </Stack>
                            </Grid>
                            <Grid item xs={2}>
                                <Button
                                    disabled={isLoading}
                                    onClick={handleTest}
                                    autoFocus
                                    variant="contained"
                                    size="large"
                                    color="success"
                                    startIcon={<TestIcon />}
                                    label={translate('action.test')}
                                />
                            </Grid>
                            {token?.access_token && (
                                <Grid item xs={12} my={2}>
                                    <Labeled>
                                        <IdField
                                            record={{
                                                token: token.access_token,
                                            }}
                                            source="token"
                                            format={t =>
                                                t.replace(/(.{80})/g, '$1\n')
                                            }
                                            copy
                                        />
                                    </Labeled>
                                </Grid>
                            )}
                            {token?.access_token_decoded && (
                                <AceEditor
                                    value={token.access_token_decoded}
                                    mode={'json'}
                                    theme={'solarized_dark'}
                                    wrapEnabled
                                    width={'100%'}
                                    setOptions={{
                                        readOnly: true,
                                        useWorker: false,
                                        showPrintMargin: false,
                                        showLineNumbers: false,
                                    }}
                                />
                            )}{' '}
                        </Grid>
                    </Box>
                )}
            </FormControl>
        </DialogContent>
    );
};

export type TestDialogButtonProps<RecordType extends RaRecord = any> = Omit<
    ButtonProps,
    'children'
> & {
    /**
     * (Optional) Custom icon for the button
     */
    icon?: ReactElement;
    /**
     * (Optional) record object to use in place of the context
     */
    record?: RecordType;
    /**
     * (Optional) resource identifier to use in place of the context
     */
    resource?: string;
    /**
     * Display the modal window as full-width, filling the viewport. Defaults to `false`
     */
    fullWidth?: boolean;
    /**
     * Max width for the modal window (breakpoint). Defaults to `md`
     */
    maxWidth?: Breakpoint;
};

const PREFIX = 'AACTestDialogButton';

export const TestDialogButtonClasses = {
    button: `${PREFIX}-button`,
    dialog: `${PREFIX}-dialog`,
    header: `${PREFIX}-header`,
    title: `${PREFIX}-title`,
    closeButton: `${PREFIX}-close-button`,
};

const TestDialog = styled(Dialog, {
    name: PREFIX,
    overridesResolver: (_props, styles) => styles.root,
})(({ theme }) => ({
    [`& .${TestDialogButtonClasses.title}`]: {
        padding: theme.spacing(0),
    },
    [`& .${TestDialogButtonClasses.header}`]: {
        padding: theme.spacing(2, 2),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    [`& .${TestDialogButtonClasses.closeButton}`]: {
        height: 'fit-content',
    },
}));
