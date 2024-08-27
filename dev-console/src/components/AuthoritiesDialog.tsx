import {
    Box,
    Breakpoint,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    FormControl,
    FormGroup,
    FormLabel,
    Grid,
    IconButton,
    Stack,
    styled,
    Switch,
    Typography,
} from '@mui/material';
import {
    Fragment,
    ReactElement,
    useCallback,
    useEffect,
    useState,
} from 'react';
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
} from 'react-admin';
import { useRootSelector } from '@dslab/ra-root-selector';

import LocalPoliceIcon from '@mui/icons-material/LocalPolice';
import CloseIcon from '@mui/icons-material/Close';
import CancelIcon from '@mui/icons-material/ErrorOutline';
import ConfirmIcon from '@mui/icons-material/CheckCircle';

const defaultIcon = <LocalPoliceIcon />;
export const AuthoritiesIcon = LocalPoliceIcon;

export const AuthoritiesDialogButton = (
    props: AuthoritiesDialogButtonProps
) => {
    const {
        label = 'action.authorities',
        icon = defaultIcon,
        fullWidth = true,
        maxWidth = 'sm',
        color = 'warning',
        record: recordFromProps,
        resource: resourceFromProps,
        onSuccess,
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
            <AuthoritiesDialog
                open={open}
                onClose={handleDialogClose}
                onClick={handleClick}
                fullWidth={fullWidth}
                maxWidth={maxWidth}
            >
                <div className={AuthoritiesDialogButtonClasses.header}>
                    <DialogTitle
                        id="authorities-dialog-title"
                        className={AuthoritiesDialogButtonClasses.title}
                    >
                        {translate(label)}
                    </DialogTitle>
                    <IconButton
                        aria-label={translate('ra.action.close')}
                        title={translate('ra.action.close')}
                        onClick={handleDialogClose}
                        size="small"
                        className={AuthoritiesDialogButtonClasses.closeButton}
                    >
                        <CloseIcon fontSize="small" />
                    </IconButton>
                </div>

                {isLoading ? (
                    <LoadingIndicator />
                ) : (
                    <AuthoritiesEditDialog
                        record={record}
                        resource={resource}
                        handleClose={handleDialogClose}
                        onSuccess={onSuccess}
                    />
                )}
            </AuthoritiesDialog>
        </Fragment>
    );
};

const AuthoritiesEditDialog = (props: {
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
    const [authorities, setAuthorities] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    //hard-coded
    const roles = ['ROLE_DEVELOPER', 'ROLE_ADMIN'];

    useEffect(() => {
        if (record) {
            dataProvider
                .invoke({
                    path:
                        resource +
                        '/' +
                        realmId +
                        '/' +
                        record.id +
                        '/authorities',
                })
                .then(data => {
                    if (data) {
                        setAuthorities(data);
                    }
                });
        }
        return () => {
            setIsLoading(false);
        };
    }, [dataProvider, record]);

    const handleSwitch = role => {
        return (event: React.ChangeEvent<HTMLInputElement>) => {
            setAuthorities(list => {
                const keep = list.filter(e => e.role !== role);
                const edit = event.target.checked
                    ? [{ realm: realmId, role: role }]
                    : [];
                return [...keep, ...edit];
            });
        };
    };

    const handleSave = e => {
        dataProvider
            .invoke({
                path:
                    resource + '/' + realmId + '/' + record.id + '/authorities',
                body: JSON.stringify(authorities),
                options: {
                    method: 'PUT',
                },
            })
            .then(() => {
                notify('ra.notification.updated');
                if (onSuccess) {
                    onSuccess();
                }
            });

        handleClose(e);
    };

    if (!record || !resource) return null;
    if (isLoading) return <LoadingIndicator />;

    return (
        <>
            <DialogContent>
                <FormControl component="fieldset" fullWidth>
                    <FormLabel component="legend" sx={{ mb: 2 }}>
                        {translate('dialog.authorities.helperText')}
                    </FormLabel>
                    <FormGroup>
                        {roles.map(r => {
                            const checked =
                                authorities.find(a => a.role === r) || false;

                            return (
                                <Box key={r} mb={2}>
                                    <Grid container mb={1}>
                                        <Grid item xs={10}>
                                            <Stack rowGap={1}>
                                                <Typography variant="h6">
                                                    {translate(
                                                        'authorities.' +
                                                            r +
                                                            '.name'
                                                    )}
                                                </Typography>
                                                <Typography
                                                    variant="body2"
                                                    fontFamily={'monospace'}
                                                >
                                                    {r}
                                                </Typography>
                                                <Typography variant="body2">
                                                    {translate(
                                                        'authorities.' +
                                                            r +
                                                            '.description'
                                                    )}
                                                </Typography>
                                            </Stack>
                                        </Grid>
                                        <Grid item xs={2}>
                                            <Switch
                                                checked={checked}
                                                onChange={handleSwitch(r)}
                                                name={r}
                                            />
                                        </Grid>
                                    </Grid>
                                    <Divider />
                                </Box>
                            );
                        })}
                    </FormGroup>
                </FormControl>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={isLoading}
                    onClick={handleClose}
                    startIcon={<CancelIcon />}
                    label={translate('ra.action.cancel')}
                />
                <Button
                    disabled={isLoading}
                    onClick={handleSave}
                    autoFocus
                    startIcon={<ConfirmIcon />}
                    label={translate('ra.action.save')}
                />
            </DialogActions>
        </>
    );
};

export type AuthoritiesDialogButtonProps<RecordType extends RaRecord = any> =
    Omit<ButtonProps, 'children'> & {
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
        onSuccess?: () => void;
    };

const PREFIX = 'AACAuthoritiesDialogButton';

export const AuthoritiesDialogButtonClasses = {
    button: `${PREFIX}-button`,
    dialog: `${PREFIX}-dialog`,
    header: `${PREFIX}-header`,
    title: `${PREFIX}-title`,
    closeButton: `${PREFIX}-close-button`,
};

const AuthoritiesDialog = styled(Dialog, {
    name: PREFIX,
    overridesResolver: (_props, styles) => styles.root,
})(({ theme }) => ({
    [`& .${AuthoritiesDialogButtonClasses.title}`]: {
        padding: theme.spacing(0),
    },
    [`& .${AuthoritiesDialogButtonClasses.header}`]: {
        padding: theme.spacing(2, 2),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    [`& .${AuthoritiesDialogButtonClasses.closeButton}`]: {
        height: 'fit-content',
    },
}));
