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
    IconButtonWithTooltip,
} from 'react-admin';
import { useRootSelector } from '@dslab/ra-root-selector';
import CallToActionIcon from '@mui/icons-material/CallToAction';
import CloseIcon from '@mui/icons-material/Close';
import CancelIcon from '@mui/icons-material/ErrorOutline';
import ConfirmIcon from '@mui/icons-material/CheckCircle';

const defaultIcon = <CallToActionIcon />;

export const ConfirmDialogButton = (props: ConfirmDialogButtonProps) => {
    const {
        label = 'action.confirm',
        title = label,
        helperText = 'page.confirm.helperText',
        icon = defaultIcon,
        fullWidth = true,
        maxWidth = 'sm',
        color = 'warning',
        record: recordFromProps,
        resource: resourceFromProps,
        onConfirm,
        onCancel,
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
        if (onCancel) {
            onCancel();
        }
    };
    const handleClick = useCallback(e => {
        e.stopPropagation();
    }, []);

    const handleConfirm = e => {
        setOpen(false);
        e.stopPropagation();
        onConfirm();
    };

    return (
        <Fragment>
            {label ? (
                <Button
                    label={label}
                    onClick={handleDialogOpen}
                    color={color}
                    {...rest}
                >
                    {icon}
                </Button>
            ) : (
                <IconButtonWithTooltip
                    label={title}
                    onClick={handleDialogOpen}
                    color={color}
                    {...rest}
                >
                    {icon}
                </IconButtonWithTooltip>
            )}
            <ConfirmDialog
                open={open}
                onClose={handleDialogClose}
                onClick={handleClick}
                fullWidth={fullWidth}
                maxWidth={maxWidth}
            >
                <div className={ConfirmDialogButtonClasses.header}>
                    <DialogTitle
                        id="confirm-dialog-title"
                        className={ConfirmDialogButtonClasses.title}
                    >
                        {translate(title)}
                    </DialogTitle>
                    <IconButton
                        aria-label={translate('ra.action.close')}
                        title={translate('ra.action.close')}
                        onClick={handleDialogClose}
                        size="small"
                        className={ConfirmDialogButtonClasses.closeButton}
                    >
                        <CloseIcon fontSize="small" />
                    </IconButton>
                </div>
                <DialogContent>
                    <FormControl component="fieldset" fullWidth>
                        <FormLabel component="legend" sx={{ mb: 2 }}>
                            {translate(helperText)}
                        </FormLabel>
                    </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={isLoading}
                        onClick={handleDialogClose}
                        startIcon={<CancelIcon />}
                        label={translate('ra.action.cancel')}
                    />
                    <Button
                        disabled={isLoading}
                        onClick={handleConfirm}
                        autoFocus
                        startIcon={<ConfirmIcon />}
                        label={translate('ra.action.confirm')}
                    />
                </DialogActions>
            </ConfirmDialog>
        </Fragment>
    );
};

export type ConfirmDialogButtonProps<RecordType extends RaRecord = any> = Omit<
    ButtonProps,
    'children'
> & {
    helperText?: string;
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
    onConfirm: () => void;
    onCancel?: () => void;
};

const PREFIX = 'AACConfirmDialogButton';

export const ConfirmDialogButtonClasses = {
    button: `${PREFIX}-button`,
    dialog: `${PREFIX}-dialog`,
    header: `${PREFIX}-header`,
    title: `${PREFIX}-title`,
    closeButton: `${PREFIX}-close-button`,
};

const ConfirmDialog = styled(Dialog, {
    name: PREFIX,
    overridesResolver: (_props, styles) => styles.root,
})(({ theme }) => ({
    [`& .${ConfirmDialogButtonClasses.title}`]: {
        padding: theme.spacing(0),
    },
    [`& .${ConfirmDialogButtonClasses.header}`]: {
        padding: theme.spacing(2, 2),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    [`& .${ConfirmDialogButtonClasses.closeButton}`]: {
        height: 'fit-content',
    },
}));
