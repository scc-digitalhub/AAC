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
import RemoveRedEye from '@mui/icons-material/RemoveRedEye';
import CloseIcon from '@mui/icons-material/Close';
import CancelIcon from '@mui/icons-material/ErrorOutline';
import ConfirmIcon from '@mui/icons-material/CheckCircle';
import { useWatch } from 'react-hook-form';

const defaultIcon = <RemoveRedEye />;

export const TemplatesPreviewButton = (props: TemplatesPreviewButtonProps) => {
    const {
        label = 'action.preview',
        title = label,
        subtitle = 'page.templates.preview.subtitle',
        icon = defaultIcon,
        fullWidth = true,
        maxWidth = 'lg',
        color = 'warning',
        record: recordFromProps,
        resource: resourceFromProps,
        ...rest
    } = props;
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const translate = useTranslate();
    const [open, setOpen] = useState(false);
    const [preview, setPreview] = useState<string>();
    const resourceContext = useResourceContext();
    const recordContext = useRecordContext();

    const resource = resourceFromProps || resourceContext;
    const record = recordFromProps || recordContext;
    const content = useWatch({ name: 'content' });

    const isLoading = !record || !content || !preview;

    const handleDialogOpen = e => {
        const r = { ...record, content };
        dataProvider
            .client(
                'templates/' + realmId + '/models/' + record.id + '/preview',
                {
                    body: JSON.stringify(r),
                    method: 'POST',
                    headers: new Headers({ Accept: 'text/html' }),
                }
            )
            .then(({ headers, body, json }) => {
                setPreview(body);
            });

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
            <TemplatesPreview
                open={open}
                onClose={handleDialogClose}
                onClick={handleClick}
                fullWidth={fullWidth}
                maxWidth={maxWidth}
            >
                <div className={TemplatesPreviewButtonClasses.header}>
                    <DialogTitle
                        id="preview-dialog-title"
                        className={TemplatesPreviewButtonClasses.title}
                    >
                        {translate(title)}
                    </DialogTitle>
                    <IconButton
                        aria-label={translate('ra.action.close')}
                        title={translate('ra.action.close')}
                        onClick={handleDialogClose}
                        size="small"
                        className={TemplatesPreviewButtonClasses.closeButton}
                    >
                        <CloseIcon fontSize="small" />
                    </IconButton>
                </div>
                <DialogContent>
                    <FormControl component="fieldset" fullWidth>
                        <FormLabel component="legend" sx={{ mb: 2 }}>
                            {translate(subtitle)}
                        </FormLabel>
                    </FormControl>
                    <div>
                        {isLoading ? (
                            <LoadingIndicator />
                        ) : (
                            <div
                                dangerouslySetInnerHTML={{
                                    __html: preview,
                                }}
                            />
                        )}
                    </div>
                </DialogContent>
            </TemplatesPreview>
        </Fragment>
    );
};

export type TemplatesPreviewButtonProps<RecordType extends RaRecord = any> =
    Omit<ButtonProps, 'children'> & {
        subtitle?: string;
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

const PREFIX = 'AACTemplatesPreviewButton';

export const TemplatesPreviewButtonClasses = {
    button: `${PREFIX}-button`,
    dialog: `${PREFIX}-dialog`,
    header: `${PREFIX}-header`,
    title: `${PREFIX}-title`,
    closeButton: `${PREFIX}-close-button`,
};

const TemplatesPreview = styled(Dialog, {
    name: PREFIX,
    overridesResolver: (_props, styles) => styles.root,
})(({ theme }) => ({
    [`& .${TemplatesPreviewButtonClasses.title}`]: {
        padding: theme.spacing(0),
    },
    [`& .${TemplatesPreviewButtonClasses.header}`]: {
        padding: theme.spacing(2, 2),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    [`& .${TemplatesPreviewButtonClasses.closeButton}`]: {
        height: 'fit-content',
    },
}));
