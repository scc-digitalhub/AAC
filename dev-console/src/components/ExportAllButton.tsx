import { downloadYaml, toYaml } from '@dslab/ra-export-record-button';
import { ReactElement } from 'react';
import {
    Button,
    ButtonProps,
    useDataProvider,
    useNotify,
    useResourceContext,
} from 'react-admin';
import DownloadIcon from '@mui/icons-material/GetApp';
import { useRootSelector } from '@dslab/ra-root-selector';

const defaultIcon = <DownloadIcon />;

/*
 * Export the whole list from GET /{resource}/{realm}/export:
 * the server returns the elements under the same list key used by RealmConfig and by the import
 */
export const ExportAllButton = (props: ExportAllButtonProps) => {
    const {
        label = 'ra.action.export',
        icon = defaultIcon,
        color = 'info',
        filename,
        resource: resourceProp,
        ...rest
    } = props;
    const dataProvider = useDataProvider();
    const notify = useNotify();
    const { root: realmId } = useRootSelector();
    const resource = useResourceContext(props);

    const handleExport = e => {
        dataProvider
            .invoke({ path: resource + '/' + realmId + '/export' })
            .then(data => {
                if (data) {
                    // the response has a single entry: the list under its RealmConfig key
                    const key = Object.keys(data)[0] || resource;
                    const name = filename || `${key}-${realmId}`;
                    downloadYaml(toYaml(data), name);
                }
            })
            .catch(error => {
                notify(
                    typeof error === 'string'
                        ? error
                        : error?.message || 'ra.notification.http_error',
                    { type: 'error' }
                );
            });

        e.stopPropagation();
    };

    return (
        <Button label={label} onClick={handleExport} color={color} {...rest}>
            {icon}
        </Button>
    );
};

export type ExportAllButtonProps = ButtonProps & {
    resource?: string;
    filename?: string;
    icon?: ReactElement;
};
